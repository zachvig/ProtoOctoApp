package de.crysxd.octoapp.base.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.annotation.Keep
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.snackbar.Snackbar

const val MAX_TIME_BETWEEN_STEPS = 300L

/**
 * A [CoordinatorLayout.Behavior] to shrink a view when e.g. a [Snackbar] is shown
 */
@Keep
class ShrinkBehavior constructor(context: Context? = null, attrs: AttributeSet? = null) :
    CoordinatorLayout.Behavior<View?>(context, attrs) {

    private var lastShrink = 0L

    override fun layoutDependsOn(
        parent: CoordinatorLayout,
        child: View,
        dependency: View
    ): Boolean {
        return dependency is Snackbar.SnackbarLayout
    }

    override fun onDependentViewChanged(
        parent: CoordinatorLayout,
        child: View,
        dependency: View
    ): Boolean {
        val shrinkBy = Math.min(0f, dependency.translationY - dependency.height)

        // The first shrink sometimes jumps to the end value before animating smoothly
        // We skip the first shrink if it is not null
        if ((System.currentTimeMillis() - lastShrink) < MAX_TIME_BETWEEN_STEPS || shrinkBy == 0f) {
            child.setPadding(
                child.paddingLeft,
                child.paddingTop,
                child.paddingRight,
                -shrinkBy.toInt()
            )
        }

        lastShrink = System.currentTimeMillis()
        return true
    }

    override fun onDependentViewRemoved(parent: CoordinatorLayout, child: View, dependency: View) {
        child.setPadding(
            child.paddingLeft,
            child.paddingTop,
            child.paddingRight,
            0
        )
    }
}
