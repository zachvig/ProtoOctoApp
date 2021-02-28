package de.crysxd.octoapp.base.ui.widget

import androidx.annotation.LayoutRes
import de.crysxd.octoapp.base.ui.base.BaseFragment

abstract class WidgetHostFragment(@LayoutRes layout: Int = 0) : BaseFragment(layout) {

    abstract fun reloadWidgets()

}