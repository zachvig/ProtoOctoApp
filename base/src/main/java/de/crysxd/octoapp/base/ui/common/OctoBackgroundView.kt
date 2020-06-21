package de.crysxd.octoapp.base.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageView
import de.crysxd.octoapp.base.R

class OctoBackgroundView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, style: Int = 0) : FrameLayout(context, attrs, style) {

    init {
        this.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS

        addView(ImageView(context).also {
            it.setImageResource(R.drawable.water_background)
            it.adjustViewBounds = true
        })

        addView(ImageView(context).also {
            it.setImageResource(R.drawable.water_background)
            it.rotation = 180f
            it.startAnimation(AnimationUtils.loadAnimation(context, R.anim.fade_in_out_loop))
            it.adjustViewBounds = true
        })
    }
}