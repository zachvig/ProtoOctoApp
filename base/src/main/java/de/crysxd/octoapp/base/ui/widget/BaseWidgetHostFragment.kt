package de.crysxd.octoapp.base.ui.widget

import de.crysxd.octoapp.base.ui.base.BaseFragment

abstract class BaseWidgetHostFragment: BaseFragment() {
    abstract fun requestTransition()
    abstract fun reloadWidgets()
}