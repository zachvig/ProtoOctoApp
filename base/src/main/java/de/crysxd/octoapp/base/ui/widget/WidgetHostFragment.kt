package de.crysxd.octoapp.base.ui.widget

import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.transition.TransitionManager
import de.crysxd.octoapp.base.ui.base.BaseFragment

abstract class WidgetHostFragment(@LayoutRes layout: Int = 0) : BaseFragment(layout) {

    abstract fun reloadWidgets()
    fun requestTransition() {
        TransitionManager.beginDelayedTransition(view as ViewGroup)
    }
}