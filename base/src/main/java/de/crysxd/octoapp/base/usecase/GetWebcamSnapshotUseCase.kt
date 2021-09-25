package de.crysxd.octoapp.base.usecase

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import androidx.core.graphics.applyCanvas
import de.crysxd.octoapp.base.data.models.OctoPrintInstanceInformationV3
import de.crysxd.octoapp.base.data.repository.OctoPrintRepository
import de.crysxd.octoapp.base.network.MjpegConnection2
import de.crysxd.octoapp.base.utils.measureTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import timber.log.Timber
import javax.inject.Inject

@Suppress("EXPERIMENTAL_API_USAGE")
class GetWebcamSnapshotUseCase @Inject constructor(
    private val getWebcamSettingsUseCase: GetWebcamSettingsUseCase,
    private val applyWebcamTransformationsUseCase: ApplyWebcamTransformationsUseCase,
    private val octoPrintRepository: OctoPrintRepository,
    private val handleAutomaticLightEventUseCase: HandleAutomaticLightEventUseCase,
) : UseCase<GetWebcamSnapshotUseCase.Params, Flow<Bitmap>>() {

    override suspend fun doExecute(param: Params, timber: Timber.Tree) = withContext(Dispatchers.IO) {
        withTimeout(10000) {
            // Get webcam settings.
            val instanceInfo = param.instanceInfo ?: octoPrintRepository.getActiveInstanceSnapshot() ?: throw IllegalStateException("No instance info")
            val activeIndex = param.instanceInfo?.appSettings?.activeWebcamIndex ?: 0
            val allWebcamSettings = getWebcamSettingsUseCase.execute(instanceInfo)
            val webcamSettings = allWebcamSettings?.getOrElse(activeIndex) { allWebcamSettings.firstOrNull() }
            var illuminated = false

            // Load single frame
            val mjpegConnection = MjpegConnection2(webcamSettings?.streamUrl?.toHttpUrlOrNull() ?: throw IllegalStateException("No stream URL"), "widget")
            mjpegConnection.load().mapNotNull { it as? MjpegConnection2.MjpegSnapshot.Frame }.map {
                measureTime("transform_frame_for_widget") {
                    val transformed = applyWebcamTransformationsUseCase.execute(ApplyWebcamTransformationsUseCase.Params(it.frame, settings = webcamSettings))
                    val width = transformed.width.coerceAtMost(param.maxWidthPx)
                    val height = ((width / transformed.width.toFloat()) * transformed.height).toInt()
                    val final = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    final.applyCanvas {
                        val clip = Path()
                        val paint = Paint().apply { isAntiAlias = true }
                        clip.addRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), param.cornerRadiusPx, param.cornerRadiusPx, Path.Direction.CW)
                        clipPath(clip)
                        drawBitmap(transformed, Rect(0, 0, transformed.width, transformed.height), Rect(0, 0, width, height), paint)
                    }
                    if (transformed != it.frame) transformed.recycle()
                    final
                }
            }.onStart {
                if (param.illuminateIfPossible) {
                    illuminated = handleAutomaticLightEventUseCase.executeBlocking(HandleAutomaticLightEventUseCase.Event.WebcamVisible("webcam-snapshot-uc"))
                    // Slight delay so a single snapshot is nicely lit
                    delay(500)
                }
            }.onCompletion {
                if (illuminated) {
                    // Execute blocking as a normal execute switches threads causing the task never to be done as the current scope
                    // is about to be terminated
                    handleAutomaticLightEventUseCase.executeBlocking(HandleAutomaticLightEventUseCase.Event.WebcamGone("webcam-snapshot-uc", delayAction = true))
                }
            }.sample(param.sampleRateMs)
        }
    }

    data class Params(
        val instanceInfo: OctoPrintInstanceInformationV3?,
        val maxWidthPx: Int,
        val sampleRateMs: Long,
        val cornerRadiusPx: Float,
        val illuminateIfPossible: Boolean
    )
}