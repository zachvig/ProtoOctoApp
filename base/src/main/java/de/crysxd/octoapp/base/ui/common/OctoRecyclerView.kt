package de.crysxd.octoapp.base.ui.common

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.util.Property
import android.view.animation.DecelerateInterpolator
import androidx.annotation.StyleRes
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.ui.OctoActivity
import timber.log.Timber
import kotlin.math.roundToInt

class OctoRecyclerView @JvmOverloads constructor(context: Context, attributeSet: AttributeSet? = null, @StyleRes defStyle: Int = 0) :
    RecyclerView(context, attributeSet, defStyle) {

    private var calculatedScrollY = 0

    private val topShadowDrawable = context.resources.getDrawable(R.drawable.top_scroll_edge_shadow, context.theme)
    private var topShadowAlpha = 0f
        set(value) {
            field = value
            invalidate()
        }
    private val topShadowAlphaProperty = object : Property<OctoRecyclerView, Float>(Float::class.java, "topShadowAlpha") {
        override fun set(view: OctoRecyclerView, value: Float) {
            view.topShadowAlpha = value
        }

        override fun get(`object`: OctoRecyclerView) = `object`.topShadowAlpha
    }
    private val bottomShadowDrawable = context.resources.getDrawable(R.drawable.bottom_scroll_edge_drawable, context.theme)
    private var bottomShadowAlpha = 0f
        set(value) {
            field = value
            invalidate()
        }
    private val bottomShadowAlphaProperty = object : Property<OctoRecyclerView, Float>(Float::class.java, "bottomShadowAlpha") {
        override fun set(view: OctoRecyclerView, value: Float) {
            view.bottomShadowAlpha = value
        }

        override fun get(`object`: OctoRecyclerView) = `object`.bottomShadowAlpha
    }

    init {
        setWillNotDraw(false)
    }

    @Suppress("DEPRECATION")
    fun setupWithToolbar(octoActivity: OctoActivity) {
        val initialState = octoActivity.octoToolbar.state
        setOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                calculatedScrollY += dy
                Timber.i("dy: $dy scrollY: $calculatedScrollY")

                octoActivity.octoToolbar.state = if (calculatedScrollY < paddingTop / 3f) {
                    if (octoActivity.octoToolbar.state == OctoToolbar.State.Hidden) {
                        octoActivity.octo.animate().alpha(1f).start()
                    }
                    initialState
                } else {
                    if (octoActivity.octoToolbar.state != OctoToolbar.State.Hidden) {
                        octoActivity.octo.animate().alpha(0f).start()
                    }
                    OctoToolbar.State.Hidden
                }

                updateViewState()
            }
        })
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        updateViewState(false)
    }

    private fun updateViewState(animated: Boolean = true) {
        val duration = if (animated) animate().duration else 0L

        val alphaTop = if (calculatedScrollY > paddingTop) 1f else 0f
        if (topShadowAlpha != alphaTop) {
            createAnimator(topShadowAlphaProperty, alphaTop, duration).start()
        }

        val alphaBottom = if (calculatedScrollY + height - paddingTop - paddingBottom < children.sumBy { it.height }) 1f else 0f
        if (bottomShadowAlpha != alphaBottom) {
            createAnimator(bottomShadowAlphaProperty, alphaBottom, duration).start()
        }
    }

    private fun createAnimator(property: Property<OctoRecyclerView, Float>, target: Float, d: Long) = ObjectAnimator.ofFloat(this, property, target).apply {
        duration = d
        interpolator = DecelerateInterpolator()
        setAutoCancel(true)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (topShadowAlpha > 0) {
            topShadowDrawable.alpha = (topShadowAlpha * 255).roundToInt()
            topShadowDrawable.setBounds(0, 0, width, topShadowDrawable.intrinsicHeight)
            topShadowDrawable.draw(canvas)
        }
        if (bottomShadowAlpha > 0) {
            bottomShadowDrawable.alpha = (bottomShadowAlpha * 255).roundToInt()
            bottomShadowDrawable.setBounds(0, height - bottomShadowDrawable.intrinsicHeight, width, height)
            bottomShadowDrawable.draw(canvas)
        }
    }
}