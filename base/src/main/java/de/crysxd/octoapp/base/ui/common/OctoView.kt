package de.crysxd.octoapp.base.ui.common

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.res.use
import androidx.core.view.children
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.ui.ColorTheme
import timber.log.Timber
import java.lang.Math.random

class OctoView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, style: Int = 0) : AppCompatImageView(context, attrs, style) {

    private var swimDrawable: Drawable? = null
    private var idleDrawable: Drawable? = null
    private var swimming = false
    private val animationHandler = Handler(Looper.getMainLooper())

    private val loopCallback = object : Animatable2Compat.AnimationCallback() {
        override fun onAnimationEnd(drawable: Drawable) {
            super.onAnimationEnd(drawable)
            if (queuedDrawable == null) {
                animationHandler.postDelayed(startRunnable, getLoopDelay(currentDrawable))
            } else {
                val q = queuedDrawable
                queuedDrawable = null
                setImageDrawable(q)
            }
        }
    }

    private var currentDrawable: Drawable? = null
    private var queuedDrawable: Drawable? = null

    private val startRunnable = Runnable {
        (drawable as? AnimatedVectorDrawableCompat)?.start()
        triggerBackgroundAction(currentDrawable)
    }

    init {
        ColorTheme.notifyAboutColorChangesUntilDetachedFromWindow(this) {
            (swimDrawable as? AnimatedVectorDrawableCompat)?.clearAnimationCallbacks()
            (idleDrawable as? AnimatedVectorDrawableCompat)?.clearAnimationCallbacks()

            swimDrawable = loadAnimatedDrawable(
                when (it.colorRes) {
                    R.color.blue_color_scheme -> R.drawable.octo_swim_blue
                    R.color.yellow_color_scheme -> R.drawable.octo_swim_yellow
                    R.color.red_color_scheme -> R.drawable.octo_swim_red
                    R.color.green_color_scheme -> R.drawable.octo_swim_green
                    R.color.violet_color_scheme -> R.drawable.octo_swim_violet
                    R.color.orange_color_scheme -> R.drawable.octo_swim_orange
                    R.color.white_color_scheme -> R.drawable.octo_swim_white
                    R.color.black_color_scheme -> R.drawable.octo_swim_black
                    else -> R.drawable.octo_swim
                }
            )
            idleDrawable = loadAnimatedDrawable(
                when (it.colorRes) {
                    R.color.blue_color_scheme -> R.drawable.octo_blink_blue
                    R.color.yellow_color_scheme -> R.drawable.octo_blink_yellow
                    R.color.red_color_scheme -> R.drawable.octo_blink_red
                    R.color.green_color_scheme -> R.drawable.octo_blink_green
                    R.color.violet_color_scheme -> R.drawable.octo_blink_violet
                    R.color.orange_color_scheme -> R.drawable.octo_blink_orange
                    R.color.white_color_scheme -> R.drawable.octo_blink_white
                    R.color.black_color_scheme -> R.drawable.octo_blink_black
                    else -> R.drawable.octo_blink
                }
            )

            (swimDrawable as? AnimatedVectorDrawableCompat)?.registerAnimationCallback(loopCallback)
            (idleDrawable as? AnimatedVectorDrawableCompat)?.registerAnimationCallback(loopCallback)

            if (swimming) {
                swim()
            } else {
                idle()
            }
        }

        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.OctoView,
            0,
            0
        ).use {
            when (it.getInt(R.styleable.OctoView_octoActivity, 0)) {
                1 -> swim()
                else -> idle()
            }
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun loadAnimatedDrawable(@DrawableRes res: Int) = AnimatedVectorDrawableCompat.create(context, res)
        ?: resources.getDrawable(res, context.theme)

    fun swim() {
        Timber.v("Swim")
        setImageDrawable(swimDrawable)
    }

    fun idle() {
        Timber.v("Idle")
        setImageDrawable(idleDrawable)
    }

    override fun setImageDrawable(drawable: Drawable?) {
        // Currently animating? Let animation finish and then swap
        if ((currentDrawable as? AnimatedVectorDrawableCompat)?.isRunning == true) {
            Timber.v("Animation active, delaying new animation")
            queuedDrawable = drawable
            return
        }
        Timber.v("Starting new animation")

        super.setImageDrawable(drawable)
        animationHandler.removeCallbacks(startRunnable)
        (swimDrawable as? AnimatedVectorDrawableCompat)?.stop()
        (idleDrawable as? AnimatedVectorDrawableCompat)?.stop()
        swimming = drawable == swimDrawable
        currentDrawable = drawable
        (drawable as? AnimatedVectorDrawableCompat)?.start()
    }

    private fun getLoopDelay(d: Drawable?) = when (d) {
        idleDrawable -> (2000 + 2000 * random()).toLong()
        else -> 0L
    }

    private fun triggerBackgroundAction(d: Drawable?) = when (d) {
        swimDrawable -> findBackgroundView()?.triggerSwimBubbles()
        else -> Unit
    }

    private fun findBackgroundView() = (parent as? ViewGroup)?.children?.firstOrNull { it is OctoBackgroundView } as OctoBackgroundView?

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) = if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.AT_MOST) {
        super.onMeasure(
            MeasureSpec.makeMeasureSpec(
                resources.getDimension(R.dimen.octo_view_width).toInt(),
                MeasureSpec.EXACTLY
            ), MeasureSpec.makeMeasureSpec(
                resources.getDimension(R.dimen.octo_view_height).toInt(),
                MeasureSpec.EXACTLY
            )
        )
    } else {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animationHandler.removeCallbacks(startRunnable)
    }
}