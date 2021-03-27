package de.crysxd.octoapp.base.ui.common

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import de.crysxd.octoapp.base.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OctoBackgroundView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, style: Int = 0) : FrameLayout(context, attrs, style) {

    private val bubblesView = ImageView(context)
    private val bubblesAnimation = AnimatedVectorDrawableCompat.create(context, R.drawable.octo_swim_bubbles)

    init {
        this.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS


        addView(ImageView(context).also {
            it.setImageResourceAsync(R.drawable.water_background)
        })

        addView(ImageView(context).also {
            it.setImageResourceAsync(R.drawable.water_background)
            it.rotation = 180f
            it.startAnimation(AnimationUtils.loadAnimation(context, R.anim.fade_in_out_loop))
            it.adjustViewBounds = true
        })

        addView(bubblesView.also {
            it.setImageDrawable(bubblesAnimation)
            it.isVisible = false
        })


        bubblesAnimation?.registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
            override fun onAnimationEnd(drawable: Drawable?) {
                bubblesView.isVisible = false
            }
        })
    }

    fun triggerSwimBubbles() {
        bubblesView.isVisible = true
        bubblesAnimation?.stop()
        bubblesAnimation?.start()
    }

    private fun ImageView.setImageResourceAsync(@DrawableRes d: Int) = GlobalScope.launch {
        val drawable = ContextCompat.getDrawable(context, d)
        withContext(Dispatchers.Main) {
            setImageDrawable(drawable)
            adjustViewBounds = true
        }
    }
}