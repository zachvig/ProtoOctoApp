package de.crysxd.baseui.common.controlcenter

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PointF
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import kotlin.math.absoluteValue

class ControlCenterLayout @JvmOverloads constructor(context: Context, attributeSet: AttributeSet? = null) : FrameLayout(context, attributeSet) {

    companion object {
        private const val MAX_ALPHA = 200
        private const val MAX_DRAG_DISTANCE = 0.33f
        private const val MIN_DRAG_DISTANCE = 0.05f
    }

    private var downPoint = PointF()
    private var dragProgress = 0f
        set(value) {
            field = value
            foreground = ColorDrawable(Color.argb((MAX_ALPHA * dragProgress).toInt(), 0, 0, 0))
        }

    override fun onInterceptTouchEvent(ev: MotionEvent) = when {
        dragProgress == 1f -> true

        ev.action == MotionEvent.ACTION_MOVE && ev.historySize > 0 && downPoint.x != 0f && downPoint.y != 0f -> {
            val distanceX = (ev.x - downPoint.x).absoluteValue
            val distanceY = (ev.y - downPoint.y).absoluteValue
            val minXDistance = width * MIN_DRAG_DISTANCE
            distanceX > distanceY * 2 && distanceX > minXDistance
        }

        ev.action == MotionEvent.ACTION_DOWN -> {
            downPoint.x = ev.x
            downPoint.y = ev.y
            false
        }

        else -> {
            dragProgress = 0f
            false
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean = when (ev.action) {
        MotionEvent.ACTION_MOVE -> {
            val distanceX = (ev.x - downPoint.x).absoluteValue
            val distanceY = (ev.y - downPoint.y).absoluteValue
            val minXDistance = width * MIN_DRAG_DISTANCE
            val maxXDistance = width * MAX_DRAG_DISTANCE

            // We consume this event if we move primarily sideways and if we moved the min distance
            val accepted = distanceX > distanceY * 2 && distanceX > minXDistance
            dragProgress = if (accepted) {
                ((distanceX - minXDistance) / maxXDistance).coerceIn(0f, 1f)
            } else {
                0f
            }

            true
        }

        MotionEvent.ACTION_UP -> {
            if (dragProgress > 0f) {
                rejectDrag()
            }
            true
        }

        else -> false
    }

    private fun rejectDrag() {
        ValueAnimator.ofFloat(dragProgress, 0f).also {
            it.addUpdateListener {
                dragProgress = it.animatedValue as Float
            }
            it.duration = 150
            it.interpolator = DecelerateInterpolator()
        }.start()
    }
}
