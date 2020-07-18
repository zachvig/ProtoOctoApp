package de.crysxd.octoapp.base.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.annotation.Keep
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.snackbar.Snackbar.SnackbarLayout
import kotlin.math.min

@Keep
class ShrinkBehaviour constructor(context: Context? = null, attrs: AttributeSet? = null) : CoordinatorLayout.Behavior<View?>(context, attrs) {


    override fun layoutDependsOn(parent: CoordinatorLayout, child: View, dependency: View): Boolean {
        return dependency is SnackbarLayout
    }

    override fun onDependentViewChanged(parent: CoordinatorLayout, child: View, dependency: View): Boolean {
        val shrinkBy = min(0f, dependency.translationY - dependency.height)
        child.setPadding(
            child.paddingLeft,
            child.paddingTop,
            child.paddingRight,
            -shrinkBy.toInt()
        )
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