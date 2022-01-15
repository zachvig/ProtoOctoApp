package de.crysxd.octoapp.base.usecase

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import androidx.core.graphics.applyCanvas
import de.crysxd.octoapp.base.data.models.OctoPrintInstanceInformationV3
import de.crysxd.octoapp.base.data.models.ResolvedWebcamSettings
import de.crysxd.octoapp.base.data.models.exceptions.SuppressedIllegalStateException
import de.crysxd.octoapp.base.data.repository.OctoPrintRepository
import de.crysxd.octoapp.base.network.MjpegConnection2
import de.crysxd.octoapp.base.utils.measureTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
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
        withTimeout(10_000) {
            // Get webcam settings.
            val instanceInfo = param.instanceInfo ?: octoPrintRepository.getActiveInstanceSnapshot() ?: throw SuppressedIllegalStateException("No instance info")
            val activeIndex = param.instanceInfo?.appSettings?.activeWebcamIndex ?: 0
            val allWebcamSettings = getWebcamSettingsUseCase.execute(instanceInfo).firstOrNull()
            val webcamSettings = allWebcamSettings?.getOrNull(activeIndex) as? ResolvedWebcamSettings.MjpegSettings
                ?: allWebcamSettings?.mapNotNull { it as? ResolvedWebcamSettings.MjpegSettings }?.firstOrNull()
            var illuminated = false

            // Load single frame'
            val mjpegConnection = MjpegConnection2(webcamSettings?.url ?: throw SuppressedIllegalStateException("No stream URL"), "widget")
            mjpegConnection.load().mapNotNull { it as? MjpegConnection2.MjpegSnapshot.Frame }.map {
                measureTime("transform_frame_for_widget") {
                    val transformed = applyWebcamTransformationsUseCase.execute(
                        ApplyWebcamTransformationsUseCase.Params(it.frame, settings = webcamSettings.webcamSettings)
                    )
                    val maxWidthScale = (param.maxSizePx / transformed.width.toFloat())
                    val maxHeightScale = (param.maxSizePx / transformed.height.toFloat())
                    val scale = minOf(maxWidthScale, maxHeightScale)
                    val width = (transformed.width * scale).toInt()
                    val height = (transformed.height * scale).toInt()
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
            }.flowOn(Dispatchers.Default).sample(param.sampleRateMs)
        }
    }

    data class Params(
        val instanceInfo: OctoPrintInstanceInformationV3?,
        val maxSizePx: Int,
        val sampleRateMs: Long,
        val cornerRadiusPx: Float,
        val illuminateIfPossible: Boolean
    )
}