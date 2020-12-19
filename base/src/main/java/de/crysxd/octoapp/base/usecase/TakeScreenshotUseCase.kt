package de.crysxd.octoapp.base.usecase

import android.app.Activity
import android.graphics.Bitmap
import android.view.View
import androidx.core.graphics.applyCanvas
import timber.log.Timber
import javax.inject.Inject

class TakeScreenshotUseCase @Inject constructor() : UseCase<Activity, Bitmap>() {

    override suspend fun doExecute(param: Activity, timber: Timber.Tree): Bitmap {
        val rootView: View = param.window.decorView.findViewById(android.R.id.content)

        val bitmap = Bitmap.createBitmap(rootView.width, rootView.height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(param.window.statusBarColor)
        bitmap.applyCanvas {
            try {
                rootView.draw(this)
            } catch (e: Exception) {
                // Well...no screenshot then
                Timber.w(e)
            }
        }

        return bitmap
    }
}