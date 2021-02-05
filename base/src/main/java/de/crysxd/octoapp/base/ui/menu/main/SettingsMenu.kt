package de.crysxd.octoapp.base.ui.menu.main

import android.content.Context
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.feedback.SendFeedbackDialog
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.base.ui.menu.*
import de.crysxd.octoapp.base.ui.menu.switchprinter.SwitchOctoPrintMenu
import de.crysxd.octoapp.base.usecase.SetAppLanguageUseCase
import kotlinx.android.parcel.Parcelize
import timber.log.Timber

@Parcelize
class SettingsMenu : Menu {
    override fun getMenuItem() = listOf(
        SendFeedbackMenuItem(),
        ChangeLanguageMenuItem(),
        NightThemeMenuItem(),
        PrintNotificationMenuItem(),
        KeepScreenOnDuringPrintMenuItem(),
        AutoConnectPrinterMenuItem(),
        ChangeOctoPrintInstanceMenuItem(),
    )

    override fun getTitle(context: Context) = context.getString(R.string.main_menu___menu_settings_title)
    override fun getSubtitle(context: Context) = context.getString(R.string.main_menu___submenu_subtitle)
    override fun getBottomText(context: Context) = HtmlCompat.fromHtml(
        context.getString(
            R.string.main_menu___about_text,
            ContextCompat.getColor(context, R.color.dark_text),
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        ),
        HtmlCompat.FROM_HTML_MODE_COMPACT
    )
}

class SendFeedbackMenuItem : MenuItem {
    override val itemId = MENU_ITEM_SEND_FEEDBACK
    override var groupId = ""
    override val order = 100
    override val enforceSingleLine = false
    override val style = MenuItemStyle.Settings
    override val icon = R.drawable.ic_round_rate_review_24

    override suspend fun getTitle(context: Context) = context.getString(R.string.main_menu___item_send_feedback)
    override suspend fun onClicked(host: MenuBottomSheetFragment, executeAsync: SuspendExecutor): Boolean {
        SendFeedbackDialog().show(host.parentFragmentManager, "feedback")
        return true
    }
}

class ChangeLanguageMenuItem : MenuItem {
    override val itemId = MENU_ITEM_CHANGE_LANGUAGE
    override var groupId = ""
    override val order = 101
    override val enforceSingleLine = false
    override val style = MenuItemStyle.Settings
    override val icon = R.drawable.ic_round_translate_24

    override suspend fun isVisible(destinationId: Int) = Injector.get().getAppLanguageUseCase().execute(Unit).canSwitchLocale
    override suspend fun getTitle(context: Context) = Injector.get().getAppLanguageUseCase().execute(Unit).switchLanguageText ?: ""
    override suspend fun onClicked(host: MenuBottomSheetFragment, executeAsync: SuspendExecutor): Boolean {
        val newLocale = Injector.get().getAppLanguageUseCase().execute(Unit).switchLanguageLocale
        Injector.get().setAppLanguageUseCase().execute(SetAppLanguageUseCase.Param(newLocale, host.requireActivity()))
        return true
    }
}

class NightThemeMenuItem : ToggleMenuItem() {
    override val isEnabled get() = Injector.get().octoPreferences().isManualDarkModeEnabled
    override val itemId = MENU_ITEM_NIGHT_THEME
    override var groupId = ""
    override val order = 103
    override val enforceSingleLine = false
    override val style = MenuItemStyle.Settings
    override val icon = R.drawable.ic_round_dark_mode_24

    override suspend fun isVisible(destinationId: Int) = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
    override suspend fun getTitle(context: Context) = context.getString(R.string.main_menu___item_use_dark_mode)
    override suspend fun handleToggleFlipped(host: MenuBottomSheetFragment, enabled: Boolean) {
        Injector.get().octoPreferences().isManualDarkModeEnabled = enabled
    }
}

class PrintNotificationMenuItem : ToggleMenuItem() {
    override val isEnabled get() = Injector.get().octoPreferences().isPrintNotificationEnabled
    override val itemId = MENU_ITEM_PRINT_NOTIFICATION
    override var groupId = ""
    override val order = 104
    override val enforceSingleLine = false
    override val style = MenuItemStyle.Settings
    override val icon = R.drawable.ic_round_notifications_active_24
    override suspend fun getTitle(context: Context) = context.getString(R.string.main_menu___item_turn_print_notification_on)

    override suspend fun handleToggleFlipped(host: MenuBottomSheetFragment, enabled: Boolean) {
        Injector.get().octoPreferences().isPrintNotificationEnabled = enabled

        try {
            if (enabled) {
                Timber.i("Service enabled, starting service")
                host.requireOctoActivity().startPrintNotificationService()
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
    override val order = 105
    override val style = MenuItemStyle.Settings
    override val icon = R.drawable.ic_round_brightness_high_24

    override suspend fun getTitle(context: Context) = context.getString(R.string.main_menu___item_keep_screen_on_during_pinrt_on)
    override suspend fun handleToggleFlipped(host: MenuBottomSheetFragment, enabled: Boolean) {
        Injector.get().octoPreferences().isKeepScreenOnDuringPrint = enabled
    }
}

class AutoConnectPrinterMenuItem : ToggleMenuItem() {
    override val isEnabled get() = Injector.get().octoPreferences().isAutoConnectPrinter
    override val itemId = MENU_ITEM_AUTO_CONNECT_PRINTER
    override var groupId = ""
    override val order = 106
    override val style = MenuItemStyle.Settings
    override val icon = R.drawable.ic_round_hdr_auto_24px

    override suspend fun getTitle(context: Context) = "Auto connect printer"
    override suspend fun handleToggleFlipped(host: MenuBottomSheetFragment, enabled: Boolean) {
        Injector.get().octoPreferences().isAutoConnectPrinter = enabled
    }
}

class ChangeOctoPrintInstanceMenuItem : MenuItem {
    override val itemId = MENU_ITEM_CHANGE_OCTOPRINT_INSTANCE
    override var groupId = ""
    override val order = 150
    override val style = MenuItemStyle.Settings
    override val enforceSingleLine = false
    override val icon = R.drawable.ic_round_swap_horiz_24
    override val showAsSubMenu = true

    override suspend fun getTitle(context: Context) = context.getString(R.string.main_menu___item_change_octoprint_instance)
    override suspend fun onClicked(host: MenuBottomSheetFragment, executeAsync: SuspendExecutor): Boolean {
        host.pushMenu(SwitchOctoPrintMenu())
        return false
    }
}