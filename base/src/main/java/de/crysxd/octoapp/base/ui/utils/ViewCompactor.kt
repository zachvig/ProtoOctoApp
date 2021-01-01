package de.crysxd.octoapp.base.ui.utils

import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnLayout
import timber.log.Timber

class ViewCompactor(val view: ViewGroup, val reset: () -> Unit, val compact: (Int) -> Boolean) {

    init {
        view.addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            val oldHeight = oldBottom - oldTop
            val oldWidth = oldRight - oldLeft
            val height = bottom - top
            val width = right - left
            if (oldHeight != 0 && (height != oldHeight || width != oldWidth)) {
                Timber.i("Layout changed")
                notifyChanged()
            }
        }
    }

    fun notifyChanged() = view.doOnLayout {
        val height = view.height - view.paddingTop - view.paddingBottom
        val width = view.width - view.paddingLeft - view.paddingRight
        Timber.i("height=$height")

        var round = -1
        reset()

        // Compact step by step until height is less than maxHeight or we have no more steps left
        while (isViewTooBig(height, width)) {
            if (!compact(++round)) {
                break
            }
        }

        view.forceLayout()
        view.invalidate()
    }

    private fun isViewTooBig(maxHeight: Int, width: Int): Boolean {
        val widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
        val heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        view.measure(widthMeasureSpec, heightMeasureSpec)
        Timber.v("Measured height: ${view.measuredHeight}")
        return view.measuredHeight > maxHeight
    }
}