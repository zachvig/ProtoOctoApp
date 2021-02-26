package de.crysxd.octoapp.base.ui.menu

abstract class SubMenuItem : MenuItem {
    abstract val subMenu: Menu
    override val showAsSubMenu = true

    override suspend fun onClicked(host: MenuBottomSheetFragment, executeAsync: SuspendExecutor): Boolean {
        host.pushMenu(subMenu)
        return false
    }
}