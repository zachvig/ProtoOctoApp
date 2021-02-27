package de.crysxd.octoapp.base.ui.menu

abstract class ToggleMenuItem : MenuItem {
    abstract val isEnabled: Boolean
    abstract suspend fun handleToggleFlipped(host: MenuBottomSheetFragment, enabled: Boolean)
    override suspend fun onClicked(host: MenuBottomSheetFragment) = throw UnsupportedOperationException("Should use handleToggleFlipped")
}