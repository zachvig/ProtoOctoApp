package de.crysxd.octoapp.base.ui.menu

import androidx.annotation.IdRes

abstract class SubMenuItem : MenuItem {
    abstract val subMenu: Menu
    override val showAsSubMenu = true

    override suspend fun isVisible(@IdRes destinationId: Int) = subMenu.getMenuItem().any { it.isVisible(destinationId) }
    override suspend fun onClicked(host: MenuBottomSheetFragment, executeAsync: SuspendExecutor): Boolean {
        host.pushMenu(subMenu)
        return false
    }
}