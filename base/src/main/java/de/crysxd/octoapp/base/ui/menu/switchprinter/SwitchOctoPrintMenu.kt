package de.crysxd.octoapp.base.ui.menu.switchprinter

import android.content.Context
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.billing.BillingManager
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.ext.toHtml
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.base.ui.menu.Menu
import de.crysxd.octoapp.base.ui.menu.MenuBottomSheetFragment
import de.crysxd.octoapp.base.ui.menu.MenuItem
import de.crysxd.octoapp.base.ui.menu.MenuItemStyle
import de.crysxd.octoapp.base.ui.menu.main.MENU_ITEM_ADD_INSTANCE
import de.crysxd.octoapp.base.ui.menu.main.MENU_ITEM_ENABLE_QUICK_SWITCH
import de.crysxd.octoapp.base.ui.menu.main.MENU_ITEM_SIGN_OUT
import de.crysxd.octoapp.base.ui.menu.main.MENU_ITEM_SWITCH_INSTANCE
import kotlinx.parcelize.Parcelize

private val isQuickSwitchEnabled get() = BillingManager.isFeatureEnabled("quick_switch")
private val isAnyActive get() = Injector.get().octorPrintRepository().getActiveInstanceSnapshot()?.webUrl != null

@Parcelize
class SwitchOctoPrintMenu : Menu {

    override suspend fun getTitle(context: Context) = context.getString(
        if (isQuickSwitchEnabled) R.string.main_menu___title_quick_switch else R.string.main_menu___title_quick_switch_disabled
    )

    override suspend fun getSubtitle(context: Context) = context.getString(
        if (isQuickSwitchEnabled) R.string.main_menu___submenu_subtitle else R.string.main_menu___subtitle_quick_switch_disabled
    )

    override fun getBottomText(context: Context) = context.getString(R.string.main_menu___hint_name_and_colorscheme).takeIf { isQuickSwitchEnabled }?.toHtml()

    override suspend fun getMenuItem() = if (isQuickSwitchEnabled) {
        val items = Injector.get().octorPrintRepository().getAll().map {
            SwitchInstanceMenuItem(webUrl = it.webUrl, showDelte = true)
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

class SwitchInstanceMenuItem(private val webUrl: String, val showDelte: Boolean = false) : MenuItem {
    companion object {
        fun forItemId(itemId: String) = SwitchInstanceMenuItem(itemId.replace(MENU_ITEM_SWITCH_INSTANCE, ""))
    }

    private val instanceInfo
        get() = Injector.get().octorPrintRepository().getAll().firstOrNull {
            it.webUrl == webUrl
        }

    override val itemId = MENU_ITEM_SWITCH_INSTANCE + webUrl
    override var groupId = ""
    override val order = 151
    override val showAsSubMenu = false
    override val style = MenuItemStyle.Settings
    override val secondaryButtonIcon = R.drawable.ic_round_delete_24.takeIf { showDelte }
    override val icon = R.drawable.ic_round_swap_horiz_24

    override suspend fun isVisible(destinationId: Int) = instanceInfo != null && isQuickSwitchEnabled &&
            Injector.get().octorPrintRepository().getActiveInstanceSnapshot()?.webUrl != webUrl

    override suspend fun getTitle(context: Context) = instanceInfo?.label ?: webUrl
    override suspend fun onClicked(host: MenuBottomSheetFragment?) {
        val repo = Injector.get().octorPrintRepository()
        instanceInfo?.let { repo.setActive(it) }
    }

    override suspend fun onSecondaryClicked(host: MenuBottomSheetFragment?) {
        host?.requireOctoActivity()?.showDialog(
            message = host.getString(R.string.main_menu___delete_octoprint_dialog_message, instanceInfo?.label ?: webUrl),
            positiveButton = host.getString(R.string.main_menu___delete_octoprint_dialog_button),
            positiveAction = {
                Injector.get().octorPrintRepository().remove(webUrl)
                host.reloadMenu()
            },
            negativeAction = {},
            negativeButton = host.getString(R.string.cancel),
        )
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
    override suspend fun onClicked(host: MenuBottomSheetFragment?) {
        Injector.get().octorPrintRepository().clearActive()
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
    override suspend fun onClicked(host: MenuBottomSheetFragment?) {
        Injector.get().octorPrintRepository().clearActive()
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
    override suspend fun onClicked(host: MenuBottomSheetFragment?) {
        OctoAnalytics.logEvent(OctoAnalytics.Event.PurchaseScreenOpen, bundleOf("trigger" to "switch_menu"))
        host?.findNavController()?.navigate(R.id.action_show_purchase_flow)
    }
}