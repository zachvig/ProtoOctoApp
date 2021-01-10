package de.crysxd.octoapp.base.ui.common.menu

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.IdRes
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.billing.BillingManager
import de.crysxd.octoapp.base.di.Injector

class MainMenu : Menu {
    override fun getMenuItem(): List<MenuItem> {
        val base = listOf(
            SupportOctoAppMenuItem(),
            ShowSettingsMenuItem(),
            ShowPrinterMenuItem(),
            ShowNewsMenuItem()
        )

        val library = MenuItemLibrary()
        val pinnedItems = Injector.get().pinnedMenuItemsRepository().getPinnedMenuItems().mapNotNull {
            library[it]
        }.mapNotNull {
            it.java.constructors[0].newInstance() as? MenuItem
        }

        return listOf(base, pinnedItems).flatten().sortedBy { it.order }
    }
}

class SupportOctoAppMenuItem : MenuItem {
    override val itemId = MENU_ITEM_SUPPORT_OCTOAPP
    override val groupId = "support"
    override val order = 0
    override val style = Style.Support
    override val showAsSubMenu = true
    override val icon = R.drawable.ic_round_favorite_24

    override suspend fun getTitle(context: Context) = context.getString(R.string.support_octoapp)
    override suspend fun isVisible(@IdRes destinationId: Int) = BillingManager.shouldAdvertisePremium()
    override suspend fun onClicked(host: MenuBottomSheetFragment): Boolean {
        OctoAnalytics.logEvent(OctoAnalytics.Event.PurchaseScreenOpen, bundleOf("trigger" to "main_menu"))
        host.findNavController().navigate(R.id.action_show_purchase_flow)
        return false
    }
}

class ShowSettingsMenuItem : SubMenuItem() {
    override val itemId = MENU_ITEM_SETTINGS_MENU
    override val groupId = "main_menu"
    override val order = 10
    override val style = Style.Settings
    override val showAsSubMenu = true
    override val showAsHalfWidth = true
    override val icon = R.drawable.ic_round_settings_24
    override val subMenu = SettingsMenu()

    override suspend fun getTitle(context: Context) = "Settings"
}

class ShowPrinterMenuItem : SubMenuItem() {
    override val itemId = MENU_ITEM_PRINTER_MENU
    override val groupId = "main_menu"
    override val order = 20
    override val style = Style.Printer
    override val showAsSubMenu = true
    override val showAsHalfWidth = true
    override val icon = R.drawable.ic_round_print_24
    override val subMenu = PrinterMenu()
    override suspend fun getTitle(context: Context) = "Printer"
}

class ShowNewsMenuItem : MenuItem {
    override val itemId = MENU_ITEM_NEWS
    override val groupId = "main_menu"
    override val order = 30
    override val style = Style.External
    override val showAsSubMenu = true
    override val showAsHalfWidth = true
    override val icon = R.drawable.ic_twitter_24px

    override suspend fun getTitle(context: Context) = "News"
    override suspend fun onClicked(host: MenuBottomSheetFragment): Boolean {
        val i = Intent(Intent.ACTION_VIEW)
        i.data = Uri.parse("https://twitter.com/realoctoapp")
        host.startActivity(i)
        return false
    }
}