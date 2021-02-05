package de.crysxd.octoapp.base.ui.menu

abstract class ToggleMenuItem : MenuItem {
    abstract val isEnabled: Boolean
    abstract suspend fun handleToggleFlipped(host: MenuBottomSheetFragment, enabled: Boolean)

}