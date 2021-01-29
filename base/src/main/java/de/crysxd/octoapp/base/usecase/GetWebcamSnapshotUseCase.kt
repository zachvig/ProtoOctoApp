package de.crysxd.octoapp.base.usecase

import android.content.Context
import android.graphics.*
import androidx.annotation.DimenRes
import androidx.core.graphics.applyCanvas
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformationV2
import de.crysxd.octoapp.base.ui.widget.webcam.MjpegConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import javax.inject.Inject

class GetWebcamSnapshotUseCase @Inject constructor(
    private val context: Context,
    private val getWebcamSettingsUseCase: GetWebcamSettingsUseCase,
    private val applyWebcamTransformationsUseCase: ApplyWebcamTransformationsUseCase,
) : UseCase<GetWebcamSnapshotUseCase.Params, Bitmap>() {

    override suspend fun doExecute(param: Params, timber: Timber.Tree) = withContext(Dispatchers.IO) {
        withTimeout(10000) {
            val streamSettings = getWebcamSettingsUseCase.execute(param.instanceInfo)
            val mjpegConnection = MjpegConnection(streamSettings.streamUrl ?: throw IllegalStateException("No stream URL"))
            val frame = mjpegConnection.load().mapNotNull { it as? MjpegConnection.MjpegSnapshot.Frame }.first().frame
            val transformed = applyWebcamTransformationsUseCase.execute(ApplyWebcamTransformationsUseCase.Params(frame, streamSettings))
            val width = transformed.width.coerceAtMost(param.maxWidthPx)
            val height = ((width / transformed.width.toFloat()) * transformed.height).toInt()
            val cornerRadius = context.resources.getDimension(param.cornerRadius)
            val final = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            final.applyCanvas {
                val clip = Path()
                val paint = Paint().apply { isAntiAlias = true }
                clip.addRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), cornerRadius, cornerRadius, Path.Direction.CW)
                clipPath(clip)
                drawBitmap(transformed, Rect(0, 0, transformed.width, transformed.height), Rect(0, 0, width, height), paint)
            }
            transformed.recycle()
            final
        }
    }

    data class Params(
        val instanceInfo: OctoPrintInstanceInformationV2?,
        val maxWidthPx: Int,
        @DimenRes val cornerRadius: Int,
    )
}