package de.crysxd.octoapp.base.ui.menu

abstract class ToggleMenuItem : MenuItem {
    abstract val isEnabled: Boolean
    abstract suspend fun handleToggleFlipped(host: MenuHost, enabled: Boolean)
    override suspend fun onClicked(host: MenuHost?) = throw UnsupportedOperationException("Should use handleToggleFlipped")
}