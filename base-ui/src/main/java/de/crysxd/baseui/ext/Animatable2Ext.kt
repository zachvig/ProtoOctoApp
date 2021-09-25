package de.crysxd.baseui.ext

import android.graphics.drawable.Animatable2
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.M)
fun Animatable2.oneOffEndAction(action: () -> Unit) {
    registerAnimationCallback(object : Animatable2.AnimationCallback() {
        override fun onAnimationEnd(drawable: Drawable?) {
            super.onAnimationEnd(drawable)
            unregisterAnimationCallback(this)
            action()
        }
    })
}