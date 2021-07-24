package de.crysxd.octoapp.base.ui.menu

abstract class SubMenuItem : MenuItem {
    abstract val subMenu: Menu
    override val showAsSubMenu = true

    override suspend fun onClicked(host: MenuHost?) {
        host?.pushMenu(subMenu)
    }
}