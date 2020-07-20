package de.crysxd.octoapp.base.usecase

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Color
import android.view.View
import androidx.core.graphics.applyCanvas
import javax.inject.Inject

class TakeScreenshotUseCase @Inject constructor() : UseCase<Activity, Bitmap> {

    override suspend fun execute(param: Activity): Bitmap {
        val rootView: View = param.window.decorView.findViewById(android.R.id.content)

        val bitmap = Bitmap.createBitmap(rootView.width, rootView.height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.WHITE)
        bitmap.applyCanvas {
            rootView.draw(this)
        }

        return bitmap
    }
}