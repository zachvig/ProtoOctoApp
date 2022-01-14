package de.crysxd.baseui.common.controlcenter

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import kotlin.math.absoluteValue

class ControlCenterHostLayout @JvmOverloads constructor(context: Context, attributeSet: AttributeSet? = null) : FrameLayout(context, attributeSet) {

    companion object {
        private const val MAX_DRAG_DISTANCE = 0.25f
        private const val MIN_DRAG_DISTANCE = 0.05f
    }

    private val controlCenterView by lazy { getChildAt(0) }
    lateinit var controlCenterFragment: Fragment
    lateinit var fragmentManager: FragmentManager

    private var dragStartPoint = PointF()
    private var dragEndPoint = PointF()
    private var dragStartProgress = 0f
    private var dragProgress = 0f
        set(value) {
            field = value
            controlCenterView.alpha = dragProgress

            val wasVisible = controlCenterView.isVisible
            val isVisible = dragProgress > 0
            controlCenterView.isVisible = isVisible
            synchronizeViewTranslationWithTouch()

            if (isVisible != wasVisible) {
                fragmentManager.beginTransaction().let { if (isVisible) it.attach(controlCenterFragment) else it.detach(controlCenterFragment) }.commit()
            }
        }

    fun disableForLifecycle(lifecycle: Lifecycle) {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onPause(owner: LifecycleOwner) {
                super.onPause(owner)
                isEnabled = true
            }

            override fun onResume(owner: LifecycleOwner) {
                super.onResume(owner)
                isEnabled = false
            }

            override fun onDestroy(owner: LifecycleOwner) {
                super.onDestroy(owner)
                lifecycle.removeObserver(this)
            }
        })
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        controlCenterView.isVisible = false
        controlCenterView.translationZ = 100f
        controlCenterView.setOnTouchListener { _, _ -> true }
        fragmentManager.beginTransaction()
            .add(controlCenterView.id, controlCenterFragment)
            .detach(controlCenterFragment)
            .commitAllowingStateLoss()
    }

    override fun onInterceptTouchEvent(ev: MotionEvent) = when {
        isEnabled && ev.action == MotionEvent.ACTION_MOVE && ev.historySize > 0 && dragStartPoint.x != 0f && dragStartPoint.y != 0f -> {
            controlCenterFragment
            ev.isAccepted()
        }

        isEnabled && ev.action == MotionEvent.ACTION_DOWN -> {
            dragStartPoint.x = ev.x
            dragStartPoint.y = ev.y
            dragStartProgress = if (controlCenterView.isVisible) 1f else 0f
            false
        }

        else -> {
            if (ev.isAccepted()) rejectDrag()
            super.onInterceptTouchEvent(ev)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean = when (ev.action) {
        MotionEvent.ACTION_MOVE -> {
            // We consume this event if we move primarily sideways and if we moved the min distance
            dragEndPoint.x = ev.x
            dragEndPoint.y = ev.y
            dragProgress = if (ev.isAccepted()) ev.dragProgress() else dragStartProgress
            true
        }

        MotionEvent.ACTION_UP -> {
            if (dragProgress != 1f && dragProgress != 0f) rejectDrag()
            true
        }

        else -> false
    }

    private fun MotionEvent.isAccepted(): Boolean {
        val distanceX = (x - dragStartPoint.x).absoluteValue
        val distanceY = (y - dragStartPoint.y).absoluteValue
        val minXDistance = width * MIN_DRAG_DISTANCE
        return distanceX > distanceY * 2 && distanceX > minXDistance
    }

    private fun MotionEvent.dragProgress(): Float {
        val distanceX = (x - dragStartPoint.x).absoluteValue
        val minXDistance = width * MIN_DRAG_DISTANCE
        val maxXDistance = width * MAX_DRAG_DISTANCE
        val progress = ((distanceX - minXDistance) / maxXDistance).coerceIn(0f, 1f)
        return if (dragStartProgress == 1f) 1 - progress else progress
    }

    private fun synchronizeViewTranslationWithTouch() {
        val minXDistance = width * MIN_DRAG_DISTANCE
        val maxXDistance = width * MAX_DRAG_DISTANCE
        val direction1 = if (dragEndPoint.x > dragStartPoint.x) -1f else 1f
        val direction2 = if (dragStartProgress == 1f) -1f else 1f
        controlCenterFragment.view?.translationX = (maxXDistance - minXDistance) * (1 - dragProgress) * direction1 * direction2
    }

    private fun rejectDrag() {
        ValueAnimator.ofFloat(dragProgress, dragStartProgress).also {
            it.addUpdateListener {
                dragProgress = it.animatedValue as Float
            }
            it.duration = 150
            it.interpolator = DecelerateInterpolator()
        }.start()
    }
}
