package de.crysxd.octoapp

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import androidx.coordinatorlayout.widget.CoordinatorLayout


class FeedbackTriggerCoordinatorLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : CoordinatorLayout(context, attrs, defStyle) {

    var onFeedbackTriggeredListener: () -> Unit = {}
    private val feedbackTriggerAreaSize = context.resources.getDimension(R.dimen.feedback_trigger_area_size)
    private val gestureDetector = GestureDetector(context, object : SimpleOnGestureListener() {
        override fun onLongPress(ev: MotionEvent) {
            val isInFeedbackTriggerArea = ev.y < feedbackTriggerAreaSize && width - feedbackTriggerAreaSize < ev.x
            if (isInFeedbackTriggerArea) {
                onFeedbackTriggeredListener()
            }
        }
    })

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(ev)
        return super.onInterceptTouchEvent(ev)
    }
}