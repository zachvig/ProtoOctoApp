package de.crysxd.octoapp.base.usecase

import android.graphics.Bitmap
import android.graphics.Matrix
import de.crysxd.octoapp.octoprint.models.settings.WebcamSettings
import timber.log.Timber
import javax.inject.Inject

class ApplyWebcamTransformationsUseCase @Inject constructor() : UseCase<ApplyWebcamTransformationsUseCase.Params, Bitmap>() {
    init {
        suppressLogging = true
    }

    override suspend fun doExecute(param: Params, timber: Timber.Tree) =
        if (param.settings.flipV || param.settings.flipH || param.settings.rotate90) {
            val matrix = Matrix()

            if (param.settings.rotate90) {
                matrix.postRotate(-90f)
            }

            matrix.postScale(
                if (param.settings.flipH) -1f else 1f,
                if (param.settings.flipV) -1f else 1f,
                param.frame.width / 2f,
                param.frame.height / 2f
            )

            val transformed = Bitmap.createBitmap(param.frame, 0, 0, param.frame.width, param.frame.height, matrix, true)
            param.frame.recycle()
            transformed
        } else {
            param.frame
        }

    data class Params(
        val frame: Bitmap,
        val settings: WebcamSettings,
        val maxWidth: Int = Int.MAX_VALUE
    )
}