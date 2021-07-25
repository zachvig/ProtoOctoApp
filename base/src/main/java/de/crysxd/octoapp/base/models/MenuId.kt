package de.crysxd.octoapp.base.models

import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.ui.menu.MenuItem

enum class MenuId {

    MainMenu,
    PrintWorkspace,
    PrePrintWorkspace,
    Widget,
    Other;

    fun canPin(menuItem: MenuItem) = (this != Widget || menuItem.canRunWithAppInBackground) && this != Other

    val label
        get() = when (this) {
            MainMenu -> R.string.menu_controls___main
            PrintWorkspace -> R.string.menu_controls___print_workspace
            PrePrintWorkspace -> R.string.menu_controls___prepare_workspace
            Widget -> R.string.menu_controls___widget
            Other -> R.string.menu_controls___other
        }
}