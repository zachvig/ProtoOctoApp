package de.crysxd.octoapp.base.ui.common.menu.switchprinter

import android.content.Context
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.billing.BillingManager
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.ui.common.menu.Menu
import de.crysxd.octoapp.base.ui.common.menu.MenuBottomSheetFragment
import de.crysxd.octoapp.base.ui.common.menu.MenuItem
import de.crysxd.octoapp.base.ui.common.menu.MenuItemStyle
import de.crysxd.octoapp.base.ui.common.menu.main.MENU_ITEM_ADD_INSTANCE
import de.crysxd.octoapp.base.ui.common.menu.main.MENU_ITEM_ENABLE_QUICK_SWITCH
import de.crysxd.octoapp.base.ui.common.menu.main.MENU_ITEM_SIGN_OUT
import de.crysxd.octoapp.base.ui.common.menu.main.MENU_ITEM_SWITCH_INSTANCE
import kotlinx.android.parcel.Parcelize

private var isQuickSwitchEnabled = BillingManager.isFeatureEnabled("quick_switch")
private val isAnyActive get() = Injector.get().octorPrintRepository().getActiveInstanceSnapshot()?.webUrl != null

@Parcelize
class SwitchOctoPrintMenu : Menu {

    override fun getTitle(context: Context) = context.getString(
        if (isQuickSwitchEnabled) R.string.main_menu___title_quick_switch else R.string.main_menu___title_quick_switch_disabled
    )

    override fun getSubtitle(context: Context) = context.getString(
        if (isQuickSwitchEnabled) R.string.main_menu___submenu_subtitle else R.string.main_menu___subtitle_quick_switch_disabled
    )

    override fun getMenuItem() = if (isQuickSwitchEnabled) {
        val items = Injector.get().octorPrintRepository().getAll().map {
            SwitchInstanceMenuItem(webUrl = it.webUrl)
        }

        val static = listOf(
            AddInstanceMenuItem()
        )

        listOf(static, items).flatten()
    } else {
        listOf(
            SignOutMenuItem(),
            EnableQuickSwitchMenuItem()
        )
    }


}

class SwitchInstanceMenuItem(private val webUrl: String) : MenuItem {
    companion object {
        fun forItemId(itemId: String) = SwitchInstanceMenuItem(itemId.replace(MENU_ITEM_SWITCH_INSTANCE, ""))
    }

    override val itemId = MENU_ITEM_SWITCH_INSTANCE + webUrl
    override var groupId = ""
    override val order = 151
    override val showAsSubMenu = false
    override val style = MenuItemStyle.Settings
    override val icon = R.drawable.ic_round_swap_horiz_24

    override suspend fun isVisible(destinationId: Int) = isQuickSwitchEnabled && Injector.get().octorPrintRepository().getActiveInstanceSnapshot()?.webUrl != webUrl
    override suspend fun getTitle(context: Context) = webUrl.replace("http://", "").replace("https://", "")
    override suspend fun onClicked(host: MenuBottomSheetFragment): Boolean {
        val repo = Injector.get().octorPrintRepository()
        repo.getAll().firstOrNull {
            it.webUrl == webUrl
        }?.let {
            repo.setActive(it)
        }
        return true
    }
}

class AddInstanceMenuItem : MenuItem {
    override val itemId = MENU_ITEM_ADD_INSTANCE
    override var groupId = ""
    override val order = 150
    override val style = MenuItemStyle.Settings
    override val icon = R.drawable.ic_round_add_24

    override suspend fun isVisible(destinationId: Int) = isQuickSwitchEnabled && isAnyActive
    override suspend fun getTitle(context: Context) = context.getString(R.string.main_menu___item_add_instance)
    override suspend fun onClicked(host: MenuBottomSheetFragment): Boolean {
        Injector.get().octorPrintRepository().clearActive()
        return true
    }
}

class SignOutMenuItem : MenuItem {
    override val itemId = MENU_ITEM_SIGN_OUT
    override var groupId = ""
    override val order = 150
    override val canBePinned = false
    override val style = MenuItemStyle.Settings
    override val icon = R.drawable.ic_round_login_24

    override suspend fun isVisible(destinationId: Int) = !isQuickSwitchEnabled && isAnyActive
    override suspend fun getTitle(context: Context) = context.getString(R.string.main_menu___item_sign_out)
    override suspend fun onClicked(host: MenuBottomSheetFragment): Boolean {
        Injector.get().octorPrintRepository().clearActive()
        return true
    }
}

class EnableQuickSwitchMenuItem : MenuItem {
    override val itemId = MENU_ITEM_ENABLE_QUICK_SWITCH
    override var groupId = ""
    override val order = 151
    override val canBePinned = false
    override val style = MenuItemStyle.Settings
    override val icon = R.drawable.ic_round_swap_horiz_24

    override suspend fun isVisible(destinationId: Int) = !isQuickSwitchEnabled
    override suspend fun getTitle(context: Context) = context.getString(R.string.main_menu___enable_quick_switch)
    override suspend fun onClicked(host: MenuBottomSheetFragment): Boolean {
        OctoAnalytics.logEvent(OctoAnalytics.Event.PurchaseScreenOpen, bundleOf("trigger" to "switch_menu"))
        host.findNavController().navigate(R.id.action_show_purchase_flow)
        isQuickSwitchEnabled = true
        return false
    }
}