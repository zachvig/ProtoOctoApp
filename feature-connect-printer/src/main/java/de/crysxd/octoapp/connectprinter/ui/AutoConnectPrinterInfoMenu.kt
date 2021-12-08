package de.crysxd.octoapp.connectprinter.ui

import android.content.Context
import de.crysxd.baseui.menu.Menu
import de.crysxd.baseui.menu.MenuHost
import de.crysxd.baseui.menu.MenuItem
import de.crysxd.baseui.menu.MenuItemStyle
import de.crysxd.octoapp.base.di.BaseInjector
import de.crysxd.octoapp.base.ext.toHtml
import de.crysxd.octoapp.connectprinter.R
import kotlinx.parcelize.Parcelize

@Parcelize
class AutoConnectPrinterInfoMenu : Menu {

    override suspend fun getTitle(context: Context) = context.getString(R.string.connect_printer___auto_menu___title)
    override suspend fun getSubtitle(context: Context) = context.getString(R.string.connect_printer___auto_menu___subtitle).toHtml()

    override suspend fun getMenuItem() = listOf(
        ConnectAutomaticallyMenuItem(),
        ConnectManuallyMenuItem(),
    )

    private class ConnectAutomaticallyMenuItem : MenuItem {
        override val itemId = "auto_connect"
        override var groupId = ""
        override val order = 1
        override val canBePinned = false
        override val style = MenuItemStyle.Settings
        override val icon = R.drawable.ic_round_auto_fix_high_24
        override fun getTitle(context: Context) = context.getString(R.string.connect_printer___auto_menu___auto_option)

        override suspend fun onClicked(host: MenuHost?) {
            BaseInjector.get().octoPreferences().wasAutoConnectPrinterInfoShown = true
            BaseInjector.get().octoPreferences().isAutoConnectPrinter = true
            host?.closeMenu()
            // Will automatically trigger connection
        }
    }

    private class ConnectManuallyMenuItem : MenuItem {
        override val itemId = "manual_connect"
        override var groupId = ""
        override val order = 2
        override val canBePinned = false
        override val style = MenuItemStyle.Settings
        override val icon = R.drawable.ic_baseline_auto_fix_off_24
        override fun getTitle(context: Context) = context.getString(R.string.connect_printer___auto_menu___manual_option)

        override suspend fun onClicked(host: MenuHost?) {
            BaseInjector.get().octoPreferences().wasAutoConnectPrinterInfoShown = true
            BaseInjector.get().octoPreferences().isAutoConnectPrinter = false
            (host?.getHostFragment() as? ConnectPrinterFragment)?.startManualConnection()
            host?.closeMenu()
        }
    }
}