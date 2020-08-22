package de.crysxd.octoapp.base.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.annotation.Keep
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.snackbar.Snackbar
import kotlin.math.min

const val MAX_TIME_BETWEEN_STEPS = 300L

/**
 * A [CoordinatorLayout.Behavior] to shrink a view when e.g. a [Snackbar] is shown
 */
@Keep
class ShrinkBehavior constructor(context: Context? = null, attrs: AttributeSet? = null) :
    CoordinatorLayout.Behavior<View?>(context, attrs) {

    private var lastShrink = 0f

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
        val shrinkBy = min(0f, dependency.translationY - dependency.height)

        // Sometimes we get odd values, drop those frames so the view doesn't jump
        if (shrinkBy == -dependency.height.toFloat() && lastShrink > -10) {
            return true
        }
        
        child.setPadding(
            child.paddingLeft,
            child.paddingTop,
            child.paddingRight,
            -shrinkBy.toInt()
        )

        lastShrink = shrinkBy
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
