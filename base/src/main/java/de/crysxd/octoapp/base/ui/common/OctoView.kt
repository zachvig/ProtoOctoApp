package de.crysxd.octoapp.base.ui.common

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.res.use
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import de.crysxd.octoapp.base.R
import java.lang.Math.random

class OctoView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, style: Int = 0) : AppCompatImageView(context, attrs, style) {

    private val swimDrawable = AnimatedVectorDrawableCompat.create(context, R.drawable.octo_swim)
        ?: resources.getDrawable(R.drawable.octo_swim, context.theme)

    private val idleDrawable = AnimatedVectorDrawableCompat.create(context, R.drawable.octo_blink)
        ?: resources.getDrawable(R.drawable.octo_blink, context.theme)

    private val loopCallback = object : Animatable2Compat.AnimationCallback() {
        override fun onAnimationEnd(drawable: Drawable) {
            super.onAnimationEnd(drawable)
            handler?.postDelayed(startRunnable, getLoopDelay(currentDrawable))
        }
    }

    private var currentDrawable: Drawable? = null

    private val startRunnable = Runnable {
        (drawable as? AnimatedVectorDrawableCompat)?.start()
    }

    init {
        (swimDrawable as? AnimatedVectorDrawableCompat)?.registerAnimationCallback(loopCallback)
        (idleDrawable as? AnimatedVectorDrawableCompat)?.registerAnimationCallback(loopCallback)

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

    @Suppress("MemberVisibilityCanBePrivate", "Unused")
    fun swim() {
        setState(swimDrawable)
    }

    @Suppress("MemberVisibilityCanBePrivate", "Unused")
    fun idle() {
        setState(idleDrawable)
    }

    private fun setState(d: Drawable?) {
        val current = drawable
        if (current is AnimatedVectorDrawableCompat && current.isRunning) {
            current.registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
                override fun onAnimationEnd(drawable: Drawable) {
                    setState(d)
                }
            })
        } else {
            setImageDrawable(d)
            currentDrawable = d
            (d as? AnimatedVectorDrawableCompat)?.start()
        }
    }

    private fun getLoopDelay(d: Drawable?) = when (d) {
        idleDrawable -> (2000 + 2000 * random()).toLong()
        else -> 0L
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) = super.onMeasure(
        MeasureSpec.makeMeasureSpec(
            resources.getDimension(R.dimen.octo_view_width).toInt(),
            MeasureSpec.EXACTLY
        ), MeasureSpec.makeMeasureSpec(
            resources.getDimension(R.dimen.octo_view_height).toInt(),
            MeasureSpec.EXACTLY
        )
    )

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler?.removeCallbacks(startRunnable)
    }
}