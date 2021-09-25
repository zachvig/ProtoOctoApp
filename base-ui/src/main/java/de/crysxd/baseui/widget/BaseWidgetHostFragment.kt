package de.crysxd.baseui.widget

import de.crysxd.baseui.BaseFragment

abstract class BaseWidgetHostFragment: BaseFragment() {
    abstract fun requestTransition(quickTransition: Boolean = false)
    abstract fun reloadWidgets()
}