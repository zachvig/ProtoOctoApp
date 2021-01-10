package de.crysxd.octoapp.base.ui.common.menu

import androidx.annotation.IdRes

abstract class SubMenuItem : MenuItem {
    abstract val subMenu: Menu

    override suspend fun isVisible(@IdRes destinationId: Int) = subMenu.getMenuItem().any { it.isVisible(destinationId) }
    override suspend fun onClicked(host: MenuBottomSheetFragment): Boolean {
        host.pushMenu(subMenu)
        return false
    }
}