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

class MainMenu : Menu {
    override fun getMenuItem() = listOf(
        SupportOctoAppMenuItem(),
        ShowSettingsMenuItem(),
        ShowPrinterMenuItem(),
        ShowNewsMenuItem()
    )
}

class SupportOctoAppMenuItem : MenuItem {
    override val itemId = "support_octoapp"
    override val groupId = "support"
    override val style = Style.Support
    override val showAsSubMenu = true
    override val icon = R.drawable.ic_round_favorite_24

    override suspend fun getTitle(context: Context) = context.getString(R.string.support_octoapp)
    override suspend fun isVisible(@IdRes destinationId: Int) = BillingManager.shouldAdvertisePremium()
    override suspend fun onClicked(host: MenuBottomSheetFragment): Boolean {
        OctoAnalytics.logEvent(OctoAnalytics.Event.PurchaseScreenOpen, bundleOf("trigger" to "main_menu"))
        host.findNavController().navigate(R.id.action_show_purchase_flow)
        return true
    }
}

class ShowSettingsMenuItem : MenuItem {
    override val itemId = "show_settings"
    override val groupId = "main_menu"
    override val style = Style.Settings
    override val showAsSubMenu = true
    override val showAsHalfWidth = true
    override val icon = R.drawable.ic_round_settings_24

    override suspend fun getTitle(context: Context) = "Settings"
    override suspend fun onClicked(host: MenuBottomSheetFragment): Boolean {
        host.pushMenu(SettingsMenu())
        return false
    }
}

class ShowPrinterMenuItem() : MenuItem {
    override val itemId = "show_print_menu"
    override val groupId = "main_menu"
    override val style = Style.Printer
    override val showAsSubMenu = true
    override val showAsHalfWidth = true
    override val icon = R.drawable.ic_round_print_24

    override suspend fun getTitle(context: Context) = "Printer"
    override suspend fun onClicked(host: MenuBottomSheetFragment): Boolean {
        //host.pushMenu(PrinterMenu())
        return false
    }
}

class ShowNewsMenuItem() : MenuItem {
    override val itemId = "show_news"
    override val groupId = "main_menu"
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