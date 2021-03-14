package de.crysxd.octoapp.base.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout

class BlockTouchFrameLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleRes: Int = 0) : FrameLayout(context, attrs, defStyleRes) {

    var isChildrenTouchEnabled = true

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return !isChildrenTouchEnabled || super.onInterceptTouchEvent(ev)
    }
}