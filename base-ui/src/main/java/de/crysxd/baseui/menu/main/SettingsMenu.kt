package de.crysxd.baseui.menu.main

import android.content.Context
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import de.crysxd.baseui.R
import de.crysxd.baseui.common.LinkClickMovementMethod
import de.crysxd.baseui.menu.Menu
import de.crysxd.baseui.menu.MenuHost
import de.crysxd.baseui.menu.MenuItem
import de.crysxd.baseui.menu.MenuItemStyle
import de.crysxd.baseui.menu.SubMenuItem
import de.crysxd.baseui.menu.ToggleMenuItem
import de.crysxd.baseui.menu.switchprinter.SwitchOctoPrintMenu
import de.crysxd.baseui.widget.WidgetHostFragment
import de.crysxd.octoapp.base.UriLibrary
import de.crysxd.octoapp.base.data.models.MenuItems.MENU_ITEM_AUTOMATIC_LIGHTS
import de.crysxd.octoapp.base.data.models.MenuItems.MENU_ITEM_AUTO_CONNECT_PRINTER
import de.crysxd.octoapp.base.data.models.MenuItems.MENU_ITEM_CHANGE_LANGUAGE
import de.crysxd.octoapp.base.data.models.MenuItems.MENU_ITEM_CONFIRM_POWER_OFF
import de.crysxd.octoapp.base.data.models.MenuItems.MENU_ITEM_CUSTOMIZE_WIDGETS
import de.crysxd.octoapp.base.data.models.MenuItems.MENU_ITEM_HELP
import de.crysxd.octoapp.base.data.models.MenuItems.MENU_ITEM_LIVE_NOTIFICATION
import de.crysxd.octoapp.base.data.models.MenuItems.MENU_ITEM_NIGHT_THEME
import de.crysxd.octoapp.base.data.models.MenuItems.MENU_ITEM_SCREEN_ON_DURING_PRINT
import de.crysxd.octoapp.base.data.models.MenuItems.MENU_ITEM_SHOW_CHANGE_OCTOPRINT_MENU
import de.crysxd.octoapp.base.di.BaseInjector
import de.crysxd.octoapp.base.ext.open
import de.crysxd.octoapp.base.usecase.SetAppLanguageUseCase
import kotlinx.coroutines.runBlocking
import kotlinx.parcelize.Parcelize

@Parcelize
class SettingsMenu : Menu {
    override suspend fun getMenuItem() = listOf(
        HelpMenuItem(),
        ChangeLanguageMenuItem(),
        NightThemeMenuItem(),
        PrintNotificationMenuItem(),
        KeepScreenOnDuringPrintMenuItem(),
        AutoConnectPrinterMenuItem(),
        ChangeOctoPrintInstanceMenuItem(),
        CustomizeWidgetsMenuItem(),
        ShowOctoAppLabMenuItem(),
        AutomaticLightsSettingsMenuItem(),
        ConfirmPowerOffSettingsMenuItem(),
    )

    override suspend fun getTitle(context: Context) = context.getString(R.string.main_menu___menu_settings_title)
    override suspend fun getSubtitle(context: Context) = context.getString(R.string.main_menu___submenu_subtitle)
    override fun getBottomText(context: Context) = HtmlCompat.fromHtml(
        context.getString(
            R.string.main_menu___about_text,
            ContextCompat.getColor(context, R.color.dark_text),
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        ),
        HtmlCompat.FROM_HTML_MODE_COMPACT
    )

    override fun getBottomMovementMethod(host: MenuHost) =
        LinkClickMovementMethod(object : LinkClickMovementMethod.OpenWithIntentLinkClickedListener(host.getMenuActivity()) {
            override fun onLinkClicked(context: Context, url: String?): Boolean {
                return if (url == "privacy") {
                    host.pushMenu(PrivacyMenu())
                    true
                } else {
                    super.onLinkClicked(context, url)
                }
            }
        })
}

class HelpMenuItem : MenuItem {
    override val itemId = MENU_ITEM_HELP
    override var groupId = ""
    override val order = 101
    override val enforceSingleLine = false
    override val style = MenuItemStyle.Settings
    override val icon = R.drawable.ic_round_help_outline_24

    override fun getTitle(context: Context) = context.getString(R.string.main_menu___item_help_faq_and_feedback)
    override suspend fun onClicked(host: MenuHost?) {
        host?.getMenuActivity()?.let {
            UriLibrary.getHelpUri().open(it)
        }
        host?.closeMenu()
    }
}

class ChangeLanguageMenuItem : MenuItem {
    override val itemId = MENU_ITEM_CHANGE_LANGUAGE
    override var groupId = ""
    override val order = 102
    override val enforceSingleLine = false
    override val style = MenuItemStyle.Settings
    override val icon = R.drawable.ic_round_translate_24

    override fun isVisible(destinationId: Int) = runBlocking {
        BaseInjector.get().getAppLanguageUseCase().execute(Unit).canSwitchLocale
    }

    override fun getTitle(context: Context) = runBlocking {
        BaseInjector.get().getAppLanguageUseCase().execute(Unit).switchLanguageText ?: ""
    }

    override suspend fun onClicked(host: MenuHost?) {
        val newLocale = BaseInjector.get().getAppLanguageUseCase().execute(Unit).switchLanguageLocale
        host?.getMenuActivity()?.let {
            BaseInjector.get().setAppLanguageUseCase().execute(SetAppLanguageUseCase.Param(newLocale, it))
        }
    }
}

class CustomizeWidgetsMenuItem : MenuItem {
    override val itemId = MENU_ITEM_CUSTOMIZE_WIDGETS
    override var groupId = ""
    override val order = 103
    override val style = MenuItemStyle.Settings
    override val enforceSingleLine = false
    override val icon = R.drawable.ic_round_person_pin_24
    override val canRunWithAppInBackground = false

    override fun isVisible(destinationId: Int) = destinationId == R.id.workspacePrePrint || destinationId == R.id.workspacePrint
    override fun getTitle(context: Context) = context.getString(R.string.main_menu___item_customize_widgets)
    override suspend fun onClicked(host: MenuHost?) {
        (host?.getHostFragment() as? WidgetHostFragment)?.startEdit()
        host?.closeMenu()
    }
}

class NightThemeMenuItem : ToggleMenuItem() {
    override val isEnabled get() = BaseInjector.get().octoPreferences().isManualDarkModeEnabled
    override val itemId = MENU_ITEM_NIGHT_THEME
    override var groupId = ""
    override val order = 104
    override val enforceSingleLine = false
    override val style = MenuItemStyle.Settings
    override val icon = R.drawable.ic_round_dark_mode_24

    override fun isVisible(destinationId: Int) = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
    override fun getTitle(context: Context) = context.getString(R.string.main_menu___item_use_dark_mode)
    override suspend fun handleToggleFlipped(host: MenuHost, enabled: Boolean) {
        BaseInjector.get().octoPreferences().isManualDarkModeEnabled = enabled
    }
}

class KeepScreenOnDuringPrintMenuItem : ToggleMenuItem() {
    override val isEnabled get() = BaseInjector.get().octoPreferences().isKeepScreenOnDuringPrint
    override val itemId = MENU_ITEM_SCREEN_ON_DURING_PRINT
    override var groupId = ""
    override val order = 105
    override val style = MenuItemStyle.Settings
    override val icon = R.drawable.ic_round_brightness_high_24

    override fun getTitle(context: Context) = context.getString(R.string.main_menu___item_keep_screen_on_during_pinrt_on)
    override suspend fun handleToggleFlipped(host: MenuHost, enabled: Boolean) {
        BaseInjector.get().octoPreferences().isKeepScreenOnDuringPrint = enabled
    }
}

class AutoConnectPrinterMenuItem : ToggleMenuItem() {
    override val isEnabled get() = BaseInjector.get().octoPreferences().isAutoConnectPrinter
    override val itemId = MENU_ITEM_AUTO_CONNECT_PRINTER
    override var groupId = ""
    override val order = 106
    override val style = MenuItemStyle.Settings
    override val icon = R.drawable.ic_round_hdr_auto_24px

    override fun getTitle(context: Context) = context.getString(R.string.main_menu___item_auto_connect_printer)
    override suspend fun handleToggleFlipped(host: MenuHost, enabled: Boolean) {
        BaseInjector.get().octoPreferences().isAutoConnectPrinter = enabled
    }
}

class PrintNotificationMenuItem : SubMenuItem() {
    override val subMenu: Menu get() = PrintNotificationsMenu()
    override val itemId = MENU_ITEM_LIVE_NOTIFICATION
    override var groupId = ""
    override val order = 107
    override val enforceSingleLine = false
    override val style = MenuItemStyle.Settings
    override val icon = R.drawable.ic_round_notifications_active_24
    override fun getTitle(context: Context) = context.getString(R.string.main_menu___item_print_notifications)
}

class AutomaticLightsSettingsMenuItem : SubMenuItem() {
    override val itemId = MENU_ITEM_AUTOMATIC_LIGHTS
    override var groupId = ""
    override val order = 108
    override val style = MenuItemStyle.Settings
    override val enforceSingleLine = false
    override val icon = R.drawable.ic_round_wb_incandescent_24
    override val subMenu: Menu get() = AutomaticLightsSettingsMenu()

    override fun getTitle(context: Context) = context.getString(R.string.main_menu___item_automatic_lights)
}

class ConfirmPowerOffSettingsMenuItem : SubMenuItem() {
    override val itemId = MENU_ITEM_CONFIRM_POWER_OFF
    override var groupId = ""
    override val order = 109
    override val style = MenuItemStyle.Settings
    override val enforceSingleLine = false
    override val icon = R.drawable.ic_round_power_24
    override val subMenu: Menu get() = ConfirmPowerOffSettingsMenu()

    override fun getTitle(context: Context) = context.getString(R.string.main_menu___item_confirm_power_off)
}

class ShowOctoAppLabMenuItem : SubMenuItem() {
    override val itemId = MENU_ITEM_SHOW_CHANGE_OCTOPRINT_MENU
    override var groupId = ""
    override val order = 110
    override val style = MenuItemStyle.Settings
    override val enforceSingleLine = false
    override val icon = R.drawable.ic_round_science_24px
    override val subMenu: Menu get() = OctoAppLabMenu()

    override fun getTitle(context: Context) = context.getString(R.string.main_menu___show_octoapp_lab)
}


class ChangeOctoPrintInstanceMenuItem : SubMenuItem() {
    override val itemId = MENU_ITEM_SHOW_CHANGE_OCTOPRINT_MENU
    override var groupId = "change"
    override val order = 151
    override val style = MenuItemStyle.Settings
    override val enforceSingleLine = false
    override val icon = R.drawable.ic_round_swap_horiz_24
    override val subMenu: Menu get() = SwitchOctoPrintMenu()

    override fun getTitle(context: Context) =
        context.getString(
            if (BaseInjector.get().octorPrintRepository().getAll().size > 1) {
                R.string.main_menu___item_change_octoprint_instance
            } else {
                R.string.main_menu___item_add_octoprint_instance
            }
        )
}
