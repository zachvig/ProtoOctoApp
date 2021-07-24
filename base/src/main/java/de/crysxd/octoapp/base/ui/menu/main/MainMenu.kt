package de.crysxd.octoapp.base.ui.menu.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.IdRes
import androidx.core.os.bundleOf
import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.UriLibrary
import de.crysxd.octoapp.base.billing.BillingManager
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.ext.open
import de.crysxd.octoapp.base.models.MenuId
import de.crysxd.octoapp.base.ui.menu.*
import kotlinx.parcelize.Parcelize

@Parcelize
class MainMenu : Menu {
    override fun shouldLoadBlocking() = true
    override suspend fun getMenuItem(): List<MenuItem> {
        val base = listOf(
            SupportOctoAppMenuItem(),
            ShowSettingsMenuItem(),
            ShowPrinterMenuItem(),
            ShowOctoPrintMenuItem(),
            ShowNewsMenuItem()
        )

        val library = MenuItemLibrary()
        val pinnedItems = Injector.get().pinnedMenuItemsRepository().getPinnedMenuItems(MenuId.MainMenu).mapNotNull {
            val item = library[it]
            item?.groupId = "pinned"
            item
        }

        return listOf(base, pinnedItems).flatten()
    }
}

class SupportOctoAppMenuItem : MenuItem {
    override val itemId = MENU_ITEM_SUPPORT_OCTOAPP
    override var groupId = "support"
    override val order = 0
    override val style = MenuItemStyle.Support
    override val showAsSubMenu = true
    override val canBePinned = false
    override val icon = R.drawable.ic_round_favorite_24

    override suspend fun getTitle(context: Context) = context.getString(R.string.main_menu___item_support_octoapp)
    override suspend fun isVisible(@IdRes destinationId: Int) = BillingManager.shouldAdvertisePremium()
    override suspend fun onClicked(host: MenuHost?) {
        OctoAnalytics.logEvent(OctoAnalytics.Event.PurchaseScreenOpen, bundleOf("trigger" to "main_menu"))
        host?.getOctoActivity()?.let {
            UriLibrary.getPurchaseUri().open(it)
        }
    }
}

class ShowSettingsMenuItem : SubMenuItem() {
    override val itemId = MENU_ITEM_SETTINGS_MENU
    override var groupId = "main_menu"
    override val order = 10
    override val style = MenuItemStyle.Settings
    override val showAsSubMenu = true
    override val showAsHalfWidth = true
    override val canBePinned = false
    override val icon = R.drawable.ic_round_settings_24
    override val subMenu = SettingsMenu()

    override suspend fun getTitle(context: Context) = context.getString(R.string.main_menu___item_show_settings)
}

class ShowPrinterMenuItem : SubMenuItem() {
    override val itemId = MENU_ITEM_PRINTER_MENU
    override var groupId = "main_menu"
    override val order = 40
    override val style = MenuItemStyle.Printer
    override val showAsSubMenu = true
    override val canBePinned = false
    override val showAsHalfWidth = true
    override val icon = R.drawable.ic_round_print_24
    override val subMenu = PrinterMenu()
    override suspend fun getTitle(context: Context) = context.getString(R.string.main_menu___item_show_printer)
}


class ShowOctoPrintMenuItem : MenuItem {
    override val itemId = MENU_ITEM_OCTOPRINT
    override var groupId = "main_menu"
    override val order = 30
    override val style = MenuItemStyle.OctoPrint
    override val showAsSubMenu = true
    override val canBePinned = false
    override val showAsHalfWidth = true
    override val icon = R.drawable.ic_octoprint_24px

    override suspend fun getTitle(context: Context) = "OctoPrint"
    override suspend fun onClicked(host: MenuHost?) {
        host?.pushMenu(OctoPrintMenu())
    }
}

class ShowNewsMenuItem : MenuItem {
    override val itemId = MENU_ITEM_NEWS
    override var groupId = "main_menu"
    override val order = 20
    override val style = MenuItemStyle.Neutral
    override val showAsSubMenu = true
    override val canBePinned = false
    override val showAsHalfWidth = true
    override val icon = R.drawable.ic_twitter_24px

    override suspend fun getTitle(context: Context) = context.getString(R.string.main_menu___item_news)
    override suspend fun onClicked(host: MenuHost?) {
        val i = Intent(Intent.ACTION_VIEW)
        i.data = Uri.parse("https://twitter.com/realoctoapp")
        host?.getOctoActivity()?.startActivity(i)
    }
}
