package de.crysxd.octoapp.base.ui.common

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.util.Property
import android.view.MotionEvent
import android.view.View
import android.view.ViewPropertyAnimator
import android.view.animation.DecelerateInterpolator
import androidx.annotation.StyleRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.NestedScrollView
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.ui.base.OctoActivity
import kotlin.math.roundToInt

class OctoScrollView @JvmOverloads constructor(context: Context, attributeSet: AttributeSet? = null, @StyleRes defStyle: Int = 0) :
    NestedScrollView(context, attributeSet, defStyle) {

    private var octoActivity: OctoActivity? = null
    private var initialState: OctoToolbar.State? = null
    var isUserInputEnabled = true
    var isBottomActionAnimationEnabled = true

    private val topShadowDrawable = ResourcesCompat.getDrawable(context.resources, R.drawable.top_scroll_edge_shadow, context.theme)
    private var topViewHeight = 0
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
    private val bottomShadowDrawable = ResourcesCompat.getDrawable(context.resources, R.drawable.bottom_scroll_edge_drawable, context.theme)
    private var bottomShadowAlpha = 0f
        set(value) {
            field = value
            invalidate()
        }
    private val bottomShadowAlphaProperty = object : Property<OctoScrollView, Float>(Float::class.java, "bottomShadowAlpha") {
        override fun set(view: OctoScrollView, value: Float) {
            view.bottomShadowAlpha = value
        }

        override fun get(`object`: OctoScrollView) = `object`.bottomShadowAlpha
    }

    init {
        setWillNotDraw(false)
        isFillViewport = true
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?) = isUserInputEnabled && super.onInterceptTouchEvent(ev)

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent?) = isUserInputEnabled && super.onTouchEvent(ev)

    @Suppress("DEPRECATION")
    fun setupWithToolbar(octoActivity: OctoActivity, bottomAction: View? = null, topView: View? = null) {
        initialState = octoActivity.octoToolbar.state
        this.octoActivity = octoActivity
        var animatedOut: Boolean? = null
        var ongoingAnimation: ViewPropertyAnimator? = null

        setOnScrollChangeListener { _: NestedScrollView?, _: Int, scrollY: Int, _: Int, oldScrollY: Int ->
            topView?.let {
                val topViewOffset = (it.top.toFloat() - (scrollY - paddingTop).coerceAtLeast(0)).coerceAtMost(0f)
                topView.translationY = -topViewOffset
            }

            // If we have a top view, draw shadow beneath it
            topViewHeight = topView?.let { it.y + it.height + paddingTop }?.toInt() ?: scrollY

            val overhang = getContentHeight() - (scrollY + height - paddingTop - paddingBottom) - (bottomAction?.height ?: 0)
            val scrollingDown = oldScrollY < scrollY
            if (isBottomActionAnimationEnabled) {
                if (overhang < 0f) {
                    animatedOut = null

                    // Cancel ongoing animations and set manually
                    ongoingAnimation?.cancel()
                    bottomAction?.translationY = (bottomAction?.height ?: 0) + overhang.toFloat()

                } else if (scrollingDown) {
                    if (animatedOut != true) {
                        animatedOut = true
                        ongoingAnimation = bottomAction?.animate()?.translationY(bottomAction.height.toFloat())
                        ongoingAnimation?.start()
                    }
                } else {
                    if (animatedOut != false) {
                        animatedOut = false
                        ongoingAnimation = bottomAction?.animate()?.translationY(0f)
                        ongoingAnimation?.start()
                    }
                }
            }

            post {
                updateViewState()
            }
        }

        post {
            updateViewState(true)
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        updateViewState(false)
    }

    private fun updateViewState(animated: Boolean = true) {
        val duration = if (animated) animate().duration else 0L

        val alphaTop = if (scrollY > paddingTop) 1f else 0f
        if (topShadowAlpha != alphaTop) {
            createAnimator(topShadowAlphaProperty, alphaTop, duration).start()
        }

        val alphaBottom = if (scrollY + height - paddingTop - paddingBottom < getContentHeight()) 1f else 0f
        if (bottomShadowAlpha != alphaBottom) {
            createAnimator(bottomShadowAlphaProperty, alphaBottom, duration).start()
        }

        octoActivity?.let { activity ->
            activity.octoToolbar.state = if (scrollY < paddingTop / 3f) {
                if (activity.octoToolbar.state == OctoToolbar.State.Hidden) {
                    activity.octo.animate().alpha(1f).start()
                }
                initialState ?: OctoToolbar.State.Hidden
            } else {
                if (activity.octoToolbar.state != OctoToolbar.State.Hidden) {
                    activity.octo.animate().alpha(0f).start()
                }
                OctoToolbar.State.Hidden
            }
        }
    }

    private fun getContentHeight() = getChildAt(0).height

    private fun createAnimator(property: Property<OctoScrollView, Float>, target: Float, d: Long) = ObjectAnimator.ofFloat(this, property, target).apply {
        duration = d
        interpolator = DecelerateInterpolator()
        setAutoCancel(true)
    }

    override fun onDrawForeground(canvas: Canvas) {
        super.onDrawForeground(canvas)

        if (topShadowAlpha > 0) {
            topShadowDrawable?.alpha = (topShadowAlpha * 255).roundToInt()
            topShadowDrawable?.setBounds(0, topViewHeight, width, topViewHeight + topShadowDrawable.intrinsicHeight)
            topShadowDrawable?.draw(canvas)
        }
        if (bottomShadowAlpha > 0) {
            bottomShadowDrawable?.alpha = (bottomShadowAlpha * 255).roundToInt()
            bottomShadowDrawable?.setBounds(0, scrollY + height - bottomShadowDrawable.intrinsicHeight, width, scrollY + height)
            bottomShadowDrawable?.draw(canvas)
        }
    }
}