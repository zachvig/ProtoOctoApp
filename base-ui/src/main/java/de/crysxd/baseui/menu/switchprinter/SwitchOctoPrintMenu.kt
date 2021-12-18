package de.crysxd.baseui.menu.switchprinter

import android.content.Context
import androidx.core.os.bundleOf
import de.crysxd.baseui.OctoActivity
import de.crysxd.baseui.R
import de.crysxd.baseui.menu.Menu
import de.crysxd.baseui.menu.MenuHost
import de.crysxd.baseui.menu.MenuItem
import de.crysxd.baseui.menu.MenuItemStyle
import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.base.UriLibrary
import de.crysxd.octoapp.base.billing.BillingManager
import de.crysxd.octoapp.base.billing.BillingManager.FEATURE_QUICK_SWITCH
import de.crysxd.octoapp.base.data.models.MenuItems.MENU_ITEM_ADD_INSTANCE
import de.crysxd.octoapp.base.data.models.MenuItems.MENU_ITEM_ENABLE_QUICK_SWITCH
import de.crysxd.octoapp.base.data.models.MenuItems.MENU_ITEM_SIGN_OUT
import de.crysxd.octoapp.base.data.models.MenuItems.MENU_ITEM_SWITCH_INSTANCE
import de.crysxd.octoapp.base.di.BaseInjector
import de.crysxd.octoapp.base.ext.open
import de.crysxd.octoapp.base.ext.toHtml
import kotlinx.parcelize.Parcelize

private val isQuickSwitchEnabled get() = BillingManager.isFeatureEnabled(FEATURE_QUICK_SWITCH)
private val isAnyActive get() = BaseInjector.get().octorPrintRepository().getActiveInstanceSnapshot()?.webUrl != null

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
        val items = BaseInjector.get().octorPrintRepository().getAll().map {
            SwitchInstanceMenuItem(instanceId = it.id, showDelte = true)
        }

        val static = listOf(
            AddInstanceMenuItem()
        )

        listOf(static, items).flatten()
    } else {
        listOf(
            AddInstanceMenuItem(),
            SignOutMenuItem(),
            EnableQuickSwitchMenuItem()
        )
    }
}

class SwitchInstanceMenuItem(private val instanceId: String, val showDelte: Boolean = false) : MenuItem {
    companion object {
        fun forItemId(itemId: String) = SwitchInstanceMenuItem(itemId.replace(MENU_ITEM_SWITCH_INSTANCE, ""))
    }

    private val instanceInfo
        get() = BaseInjector.get().octorPrintRepository().get(instanceId)

    override val itemId = MENU_ITEM_SWITCH_INSTANCE + instanceInfo?.id
    override var groupId = ""
    override val order = 151
    override val showAsSubMenu = false
    override val style = MenuItemStyle.Settings
    override val secondaryButtonIcon = R.drawable.ic_round_delete_24.takeIf { showDelte }
    override val icon = R.drawable.ic_round_swap_horiz_24

    override fun isVisible(destinationId: Int) = instanceInfo != null && isQuickSwitchEnabled &&
            BaseInjector.get().octorPrintRepository().getActiveInstanceSnapshot()?.id != instanceId

    override fun getTitle(context: Context) = context.getString(R.string.main_menu___switch_to_octoprint, instanceInfo?.label ?: "(deleted)")
    override suspend fun onClicked(host: MenuHost?) {
        val repo = BaseInjector.get().octorPrintRepository()
        instanceInfo?.let { repo.setActive(it) }
    }

    override suspend fun onSecondaryClicked(host: MenuHost?) {
        (host?.getMenuActivity() as? OctoActivity)?.showDialog(
            message = host.requireContext().getString(R.string.main_menu___delete_octoprint_dialog_message, instanceInfo?.label ?: "(deleted)"),
            positiveButton = host.requireContext().getString(R.string.main_menu___delete_octoprint_dialog_button),
            positiveAction = {
                BaseInjector.get().octorPrintRepository().remove(instanceId)
                host.reloadMenu()
            },
            negativeAction = {},
            negativeButton = host.requireContext().getString(R.string.cancel),
        )
    }
}

class AddInstanceMenuItem : MenuItem {
    override val itemId = MENU_ITEM_ADD_INSTANCE
    override var groupId = ""
    override val order = 150
    override val style = MenuItemStyle.Settings
    override val icon = R.drawable.ic_round_add_24

    override val isEnabled = isQuickSwitchEnabled
    override fun isVisible(destinationId: Int) = isAnyActive
    override fun getTitle(context: Context) = context.getString(R.string.main_menu___item_add_instance)
    override suspend fun onClicked(host: MenuHost?) {
        BaseInjector.get().octorPrintRepository().clearActive()
    }
}

class SignOutMenuItem : MenuItem {
    override val itemId = MENU_ITEM_SIGN_OUT
    override var groupId = "sign_out"
    override val order = 151
    override val canBePinned = false
    override val style = MenuItemStyle.Settings
    override val icon = R.drawable.ic_round_login_24

    override fun isVisible(destinationId: Int) = !isQuickSwitchEnabled && isAnyActive
    override fun getTitle(context: Context) = context.getString(R.string.main_menu___item_sign_out)
    override suspend fun onClicked(host: MenuHost?) {
        BaseInjector.get().octorPrintRepository().clearActive()
    }
}

class EnableQuickSwitchMenuItem : MenuItem {
    override val itemId = MENU_ITEM_ENABLE_QUICK_SWITCH
    override var groupId = ""
    override val order = 149
    override val canBePinned = false
    override val style = MenuItemStyle.Support
    override val icon = R.drawable.ic_round_favorite_24

    override fun isVisible(destinationId: Int) = !isQuickSwitchEnabled
    override fun getTitle(context: Context) = context.getString(R.string.main_menu___enable_quick_switch)
    override suspend fun onClicked(host: MenuHost?) {
        OctoAnalytics.logEvent(OctoAnalytics.Event.PurchaseScreenOpen, bundleOf("trigger" to "switch_menu"))
        host?.getMenuActivity()?.let {
            UriLibrary.getPurchaseUri().open(it)
        }
    }
}