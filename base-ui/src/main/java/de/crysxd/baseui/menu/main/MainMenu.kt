package de.crysxd.baseui.menu.main

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import de.crysxd.baseui.R
import de.crysxd.baseui.databinding.SaleHeaderViewBinding
import de.crysxd.baseui.menu.Menu
import de.crysxd.baseui.menu.MenuHost
import de.crysxd.baseui.menu.MenuItem
import de.crysxd.baseui.menu.MenuItemStyle
import de.crysxd.baseui.menu.SubMenuItem
import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.base.UriLibrary
import de.crysxd.octoapp.base.billing.BillingManager
import de.crysxd.octoapp.base.data.models.MenuId
import de.crysxd.octoapp.base.data.models.MenuItems.MENU_ITEM_NEWS
import de.crysxd.octoapp.base.data.models.MenuItems.MENU_ITEM_OCTOPRINT
import de.crysxd.octoapp.base.data.models.MenuItems.MENU_ITEM_PRINTER_MENU
import de.crysxd.octoapp.base.data.models.MenuItems.MENU_ITEM_SETTINGS_MENU
import de.crysxd.octoapp.base.data.models.MenuItems.MENU_ITEM_TUTORIALS
import de.crysxd.octoapp.base.data.models.MenuItems.MENU_ITEM_YOUTUBE
import de.crysxd.octoapp.base.di.BaseInjector
import de.crysxd.octoapp.base.ext.open
import de.crysxd.octoapp.base.ext.purchaseOffers
import de.crysxd.octoapp.base.ext.toHtml
import kotlinx.parcelize.Parcelize

@Parcelize
class MainMenu : Menu {
    override fun shouldLoadBlocking() = true
    override suspend fun getMenuItem(): List<MenuItem> {
        val base = listOf(
            ShowPrinterMenuItem(),
            ShowSettingsMenuItem(),
            ShowOctoPrintMenuItem(),
            ShowTutorialsMenuItem()
        )

        val library = MenuItemLibrary()
        val pinnedItems = BaseInjector.get().pinnedMenuItemsRepository().getPinnedMenuItems(MenuId.MainMenu).mapNotNull {
            val item = library[it]
            item?.groupId = "pinned"
            item
        }

        return listOf(base, pinnedItems).flatten()
    }

    override fun getCustomHeaderView(host: MenuHost) = if (BillingManager.shouldAdvertisePremium()) {
        SaleHeaderViewBinding.inflate(LayoutInflater.from(host.requireContext())).also {
            val config = Firebase.remoteConfig.purchaseOffers.activeConfig
            val context = host.requireContext()
            it.banner.text = config.textsWithData.launchPurchaseScreenHighlight?.toHtml()
            it.banner.isVisible = it.banner.text.isNotBlank()
            it.salesSpacer.isVisible = it.banner.isVisible
            fun refreshTime() {
                it.banner.text = config.textsWithData.launchPurchaseScreenHighlight?.toHtml()
                it.banner.postDelayed(::refreshTime, 1000)
            }
            refreshTime()

            it.item.badge.isVisible = false
            it.item.description.isVisible = false
            it.item.pin.isVisible = false
            it.item.right.isVisible = false
            it.item.toggle.isVisible = false
            it.item.right.isVisible = false
            it.item.secondaryButton.isVisible = false
            it.item.text.text = config.textsWithData.launchPurchaseScreenCta.takeUnless { i -> i.isBlank() }
                ?: context.getString(R.string.main_menu___item_support_octoapp)
            it.item.icon.setImageResource(R.drawable.ic_round_favorite_24)
            it.root.setBackgroundColor(if (it.banner.isVisible) ContextCompat.getColor(context, MenuItemStyle.Support.backgroundColor) else Color.TRANSPARENT)
            it.item.button.backgroundTintList = ContextCompat.getColorStateList(context, MenuItemStyle.Support.backgroundColor)
            it.item.icon.setColorFilter(ContextCompat.getColor(context, MenuItemStyle.Support.highlightColor))
            it.item.button.strokeColor = ColorStateList.valueOf(Color.TRANSPARENT)
            it.item.button.rippleColor = ContextCompat.getColorStateList(context, MenuItemStyle.Support.backgroundColor)
            it.item.button.setOnClickListener {
                OctoAnalytics.logEvent(OctoAnalytics.Event.PurchaseScreenOpen, bundleOf("trigger" to "main_menu"))
                UriLibrary.getPurchaseUri().open(host.getMenuActivity())
            }
        }.root
    } else {
        null
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

    override fun getTitle(context: Context) = context.getString(R.string.main_menu___item_show_settings)
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
    override fun getTitle(context: Context) = context.getString(R.string.main_menu___item_show_printer)
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

    override fun getBadgeCount() = OctoPrintMenu.getAnnouncementCounter()
    override fun getTitle(context: Context) = context.getString(R.string.main_menu___item_show_octoprint)
    override suspend fun onClicked(host: MenuHost?) {
        host?.pushMenu(OctoPrintMenu())
    }
}

class ShowTutorialsMenuItem(
    override val showAsHalfWidth: Boolean = true,
) : MenuItem {
    override val itemId = MENU_ITEM_TUTORIALS
    override var groupId = "main_menu"
    override val order = 20
    override val showAsOutlined = true
    override val style = MenuItemStyle.Neutral
    override val showAsSubMenu = true
    override val canBePinned = false
    override val icon = R.drawable.ic_round_school_24

    override fun getBadgeCount() = BaseInjector.get().tutorialsRepository().getNewTutorialsCount()
    override fun getTitle(context: Context) = context.getString(R.string.main_menu___item_show_tutorials)
    override suspend fun onClicked(host: MenuHost?) {
        host?.getMenuActivity()?.let {
            UriLibrary.getTutorialsUri().open(it)
        }
    }
}


class ShowNewsMenuItem : MenuItem {
    override val itemId = MENU_ITEM_NEWS
    override var groupId = "main_menu"
    override val order = 21
    override val showAsSubMenu = true
    override val canBePinned = false
    override val showAsOutlined = true
    override val icon = R.drawable.ic_twitter_24px
    override val style = MenuItemStyle.Neutral

    override fun getTitle(context: Context) = "Twitter"
    override suspend fun onClicked(host: MenuHost?) {
        host?.getMenuActivity()?.let {
            Uri.parse("https://twitter.com/realoctoapp").open(it)
        }
    }
}

class ShowYoutubeMenuItem : MenuItem {
    override val itemId = MENU_ITEM_YOUTUBE
    override var groupId = "main_menu"
    override val order = 22
    override val showAsSubMenu = true
    override val showAsOutlined = true
    override val canBePinned = false
    override val icon = R.drawable.ic_youtube_24px
    override val style = MenuItemStyle.Neutral

    override fun getTitle(context: Context) = "YouTube"
    override suspend fun onClicked(host: MenuHost?) {
        host?.getMenuActivity()?.let {
            Uri.parse("https://www.youtube.com/channel/UCUFZW6bxLNYxl8uFp37IdPg").open(it)
        }
    }
}
