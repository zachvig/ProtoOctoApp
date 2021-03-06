package de.crysxd.octoapp.base.ui.menu.main

import android.content.Context
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.UriLibrary
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.ext.open
import de.crysxd.octoapp.base.ui.base.OctoActivity
import de.crysxd.octoapp.base.ui.common.LinkClickMovementMethod
import de.crysxd.octoapp.base.ui.menu.*
import de.crysxd.octoapp.base.ui.menu.switchprinter.SwitchOctoPrintMenu
import de.crysxd.octoapp.base.usecase.GetPowerDevicesUseCase
import de.crysxd.octoapp.base.usecase.SetAppLanguageUseCase
import kotlinx.parcelize.Parcelize
import timber.log.Timber

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

    override suspend fun getTitle(context: Context) = context.getString(R.string.main_menu___item_help_faq_and_feedback)
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

    override suspend fun isVisible(destinationId: Int) = Injector.get().getAppLanguageUseCase().execute(Unit).canSwitchLocale
    override suspend fun getTitle(context: Context) = Injector.get().getAppLanguageUseCase().execute(Unit).switchLanguageText ?: ""
    override suspend fun onClicked(host: MenuHost?) {
        val newLocale = Injector.get().getAppLanguageUseCase().execute(Unit).switchLanguageLocale
        host?.getMenuActivity()?.let {
            Injector.get().setAppLanguageUseCase().execute(SetAppLanguageUseCase.Param(newLocale, it))
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

    override suspend fun isVisible(destinationId: Int) = destinationId == R.id.workspacePrePrint || destinationId == R.id.workspacePrint
    override suspend fun getTitle(context: Context) = context.getString(R.string.main_menu___item_customize_widgets)
    override suspend fun onClicked(host: MenuHost?) {
        host?.getWidgetHostFragment()?.startEdit()
        host?.closeMenu()
    }
}

class NightThemeMenuItem : ToggleMenuItem() {
    override val isEnabled get() = Injector.get().octoPreferences().isManualDarkModeEnabled
    override val itemId = MENU_ITEM_NIGHT_THEME
    override var groupId = ""
    override val order = 104
    override val enforceSingleLine = false
    override val style = MenuItemStyle.Settings
    override val icon = R.drawable.ic_round_dark_mode_24

    override suspend fun isVisible(destinationId: Int) = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
    override suspend fun getTitle(context: Context) = context.getString(R.string.main_menu___item_use_dark_mode)
    override suspend fun handleToggleFlipped(host: MenuHost, enabled: Boolean) {
        Injector.get().octoPreferences().isManualDarkModeEnabled = enabled
    }
}

class PrintNotificationMenuItem : ToggleMenuItem() {
    override val isEnabled get() = Injector.get().octoPreferences().isPrintNotificationEnabled
    override val itemId = MENU_ITEM_PRINT_NOTIFICATION
    override var groupId = ""
    override val order = 105
    override val enforceSingleLine = false
    override val style = MenuItemStyle.Settings
    override val icon = R.drawable.ic_round_notifications_active_24
    override suspend fun getTitle(context: Context) = context.getString(R.string.main_menu___item_turn_print_notification_on)

    override suspend fun handleToggleFlipped(host: MenuHost, enabled: Boolean) {
        Injector.get().octoPreferences().isPrintNotificationEnabled = enabled

        try {
            if (enabled) {
                Timber.i("Service enabled, starting service")
                (host.getMenuActivity() as? OctoActivity)?.startPrintNotificationService()
            }
        } catch (e: IllegalStateException) {
            // User might have closed app just in time so we can't start the service
        }
    }
}

class KeepScreenOnDuringPrintMenuItem : ToggleMenuItem() {
    override val isEnabled get() = Injector.get().octoPreferences().isKeepScreenOnDuringPrint
    override val itemId = MENU_ITEM_SCREEN_ON_DURING_PRINT
    override var groupId = ""
    override val order = 106
    override val style = MenuItemStyle.Settings
    override val icon = R.drawable.ic_round_brightness_high_24

    override suspend fun getTitle(context: Context) = context.getString(R.string.main_menu___item_keep_screen_on_during_pinrt_on)
    override suspend fun handleToggleFlipped(host: MenuHost, enabled: Boolean) {
        Injector.get().octoPreferences().isKeepScreenOnDuringPrint = enabled
    }
}

class AutoConnectPrinterMenuItem : ToggleMenuItem() {
    override val isEnabled get() = Injector.get().octoPreferences().isAutoConnectPrinter
    override val itemId = MENU_ITEM_AUTO_CONNECT_PRINTER
    override var groupId = ""
    override val order = 107
    override val style = MenuItemStyle.Settings
    override val icon = R.drawable.ic_round_hdr_auto_24px

    override suspend fun getTitle(context: Context) = context.getString(R.string.main_menu___item_auto_connect_printer)
    override suspend fun handleToggleFlipped(host: MenuHost, enabled: Boolean) {
        Injector.get().octoPreferences().isAutoConnectPrinter = enabled
    }
}

class AutomaticLightsSettingsMenuItem : SubMenuItem() {
    override val itemId = MENU_ITEM_AUTOMATIC_LIGHTS
    override var groupId = ""
    override val order = 108
    override val style = MenuItemStyle.Settings
    override val enforceSingleLine = false
    override val icon = R.drawable.ic_round_wb_incandescent_24
    override val subMenu: Menu get() = AutomaticLightsSettingsMenu()

    override suspend fun getTitle(context: Context) = context.getString(R.string.main_menu___item_automatic_lights)
}

class ConfirmPowerOffSettingsMenuItem : SubMenuItem() {
    override val itemId = MENU_ITEM_CONFIRM_POWER_OFF
    override var groupId = ""
    override val order = 109
    override val style = MenuItemStyle.Settings
    override val enforceSingleLine = false
    override val icon = R.drawable.ic_round_power_24
    override val subMenu: Menu get() = ConfirmPowerOffSettingsMenu()
    override suspend fun isVisible(destinationId: Int) = Injector.get().getPowerDevicesUseCase().execute(
        GetPowerDevicesUseCase.Params(queryState = false)
    ).isNotEmpty()

    override suspend fun getTitle(context: Context) = context.getString(R.string.main_menu___item_confirm_power_off)
}

class ShowOctoAppLabMenuItem : SubMenuItem() {
    override val itemId = MENU_ITEM_CHANGE_OCTOPRINT_INSTANCE
    override var groupId = ""
    override val order = 110
    override val style = MenuItemStyle.Settings
    override val enforceSingleLine = false
    override val icon = R.drawable.ic_round_science_24px
    override val subMenu: Menu get() = OctoAppLabMenu()

    override suspend fun getTitle(context: Context) = context.getString(R.string.main_menu___show_octoapp_lab)
}


class ChangeOctoPrintInstanceMenuItem : SubMenuItem() {
    override val itemId = MENU_ITEM_CHANGE_OCTOPRINT_INSTANCE
    override var groupId = ""
    override val order = 151
    override val style = MenuItemStyle.Settings
    override val enforceSingleLine = false
    override val icon = R.drawable.ic_round_swap_horiz_24
    override val subMenu: Menu get() = SwitchOctoPrintMenu()

    override suspend fun getTitle(context: Context) =
        context.getString(
            if (Injector.get().octorPrintRepository().getAll().size > 1) {
                R.string.main_menu___item_change_octoprint_instance
            } else {
                R.string.main_menu___item_add_octoprint_instance
            }
        )
}
