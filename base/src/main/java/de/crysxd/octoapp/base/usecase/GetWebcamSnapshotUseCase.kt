package de.crysxd.octoapp.base.usecase

import android.graphics.*
import androidx.core.graphics.applyCanvas
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformationV2
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.base.ui.widget.webcam.MjpegConnection
import de.crysxd.octoapp.base.utils.measureTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import javax.inject.Inject

@Suppress("EXPERIMENTAL_API_USAGE")
class GetWebcamSnapshotUseCase @Inject constructor(
    private val getWebcamSettingsUseCase: GetWebcamSettingsUseCase,
    private val applyWebcamTransformationsUseCase: ApplyWebcamTransformationsUseCase,
    private val octoPrintRepository: OctoPrintRepository,
    private val handleAutomaticIlluminationEventUseCase: HandleAutomaticIlluminationEventUseCase,
) : UseCase<GetWebcamSnapshotUseCase.Params, Flow<Bitmap>>() {

    override suspend fun doExecute(param: Params, timber: Timber.Tree) = withContext(Dispatchers.IO) {
        withTimeout(10000) {
            // Get webcam settings.
            val instanceInfo = param.instanceInfo ?: octoPrintRepository.getActiveInstanceSnapshot() ?: throw IllegalStateException("No instance info")
            val activeIndex = param.instanceInfo?.appSettings?.activeWebcamIndex ?: 0
            val allWebcamSettings = getWebcamSettingsUseCase.execute(instanceInfo)
            val webcamSettings = allWebcamSettings?.getOrElse(activeIndex) { allWebcamSettings.firstOrNull() }
            val authHeader = webcamSettings?.authHeader
            var illuminated = false

            // Load single frame
            val mjpegConnection = MjpegConnection(webcamSettings?.streamUrl ?: throw IllegalStateException("No stream URL"), authHeader, "widget")
            mjpegConnection.load().mapNotNull { it as? MjpegConnection.MjpegSnapshot.Frame }.sample(param.sampleRateMs).map {
                timber.i("Transforming image")
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
                    transformed.recycle()
                    timber.i("Image transformed")
                    final
                }
            }.onStart {
                if (param.illuminateIfPossible) {
                    illuminated = handleAutomaticIlluminationEventUseCase.executeBlocking(HandleAutomaticIlluminationEventUseCase.Event.WebcamVisible)
                    // Slight delay so a single snapshot is nicely lit
                    delay(500)
                }
            }.onCompletion {
                if (illuminated) {
                    // Execute blocking as a normal execute switches threads causing the task never to be done as the current scope
                    // is about to be terminated
                    handleAutomaticIlluminationEventUseCase.executeBlocking(HandleAutomaticIlluminationEventUseCase.Event.WebcamGone)
                }
            }
        }
    }

    data class Params(
        val instanceInfo: OctoPrintInstanceInformationV2?,
        val maxWidthPx: Int,
        val sampleRateMs: Long,
        val cornerRadiusPx: Float,
        val illuminateIfPossible: Boolean
    )
}