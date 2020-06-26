package de.crysxd.octoapp.base.ui.common

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.util.Property
import android.view.animation.DecelerateInterpolator
import androidx.annotation.StyleRes
import androidx.core.widget.NestedScrollView
import de.crysxd.octoapp.base.R
import kotlin.math.roundToInt

class OctoScrollView @JvmOverloads constructor(context: Context, attributeSet: AttributeSet? = null, @StyleRes defStyle: Int = 0) :
    NestedScrollView(context, attributeSet, defStyle) {

    private val topShadowDrawable = context.resources.getDrawable(R.drawable.scroll_edge_shadow, context.theme)
    private var topShadowAlpha = 0f
        set(value) {
            field = value
            invalidate()
        }
    private val topShadowAlphaProperty = object : Property<OctoScrollView, Float>(Float::class.java, "topShadowAlpha") {
        override fun set(view: OctoScrollView, value: Float) {
            view.topShadowAlpha = value
        }

        override fun get(`object`: OctoScrollView) = `object`.topShadowAlpha
    }

    init {
        setWillNotDraw(false)
    }

    fun setupWithToolbar(octoToolbar: OctoToolbar) {
        val initialState = octoToolbar.state
        setOnScrollChangeListener { _: NestedScrollView, _: Int, scrollY: Int, _: Int, _: Int ->
            octoToolbar.state = if (scrollY == 0) {
                initialState
            } else {
                OctoToolbar.State.Hidden
            }

            val alpha = if (scrollY > paddingTop) 1f else 0f
            if (topShadowAlpha != alpha) {
                ObjectAnimator.ofFloat(this, topShadowAlphaProperty, topShadowAlpha, alpha).also {
                    it.duration = 150
                    it.interpolator = DecelerateInterpolator()
                    it.setAutoCancel(true)
                    it.start()
                }
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (topShadowAlpha > 0) {
            topShadowDrawable.alpha = (topShadowAlpha * 255).roundToInt()
            topShadowDrawable.setBounds(0, scrollY, width, scrollY + topShadowDrawable.intrinsicHeight)
            topShadowDrawable.draw(canvas)
        }
    }
}