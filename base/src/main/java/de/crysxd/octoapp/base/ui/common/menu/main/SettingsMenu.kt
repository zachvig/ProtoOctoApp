package de.crysxd.octoapp.base.ui.common.menu.main

import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.feedback.SendFeedbackDialog
import de.crysxd.octoapp.base.ui.common.menu.Menu
import de.crysxd.octoapp.base.ui.common.menu.MenuBottomSheetFragment
import de.crysxd.octoapp.base.ui.common.menu.MenuItem
import de.crysxd.octoapp.base.ui.common.menu.MenuItemStyle
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.base.usecase.SetAppLanguageUseCase
import timber.log.Timber


class SettingsMenu : Menu {
    override fun getMenuItem() = listOf(
        SendFeedbackMenuItem(),
        ChangeLanguageMenuItem(),
        OpenOctoPrintMenuItem(),
        NightThemeMenuItem(),
        PrintNotificationMenuItem(),
        KeepScreenOnDuringPrintMenuItem(),
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
    override val groupId = ""
    override val order = 100
    override val style = MenuItemStyle.Settings
    override val icon = R.drawable.ic_round_rate_review_24

    override suspend fun getTitle(context: Context) = context.getString(R.string.main_menu___item_send_feedback)
    override suspend fun onClicked(host: MenuBottomSheetFragment): Boolean {
        SendFeedbackDialog().show(host.parentFragmentManager, "feedback")
        return true
    }
}

class ChangeLanguageMenuItem : MenuItem {
    override val itemId = MENU_ITEM_CHANGE_LANGUAGE
    override val groupId = ""
    override val order = 110
    override val style = MenuItemStyle.Settings
    override val icon = R.drawable.ic_round_translate_24

    override suspend fun isVisible(destinationId: Int) = Injector.get().getAppLanguageUseCase().execute(Unit).canSwitchLocale
    override suspend fun getTitle(context: Context) = Injector.get().getAppLanguageUseCase().execute(Unit).switchLanguageText ?: ""
    override suspend fun onClicked(host: MenuBottomSheetFragment): Boolean {
        val newLocale = Injector.get().getAppLanguageUseCase().execute(Unit).switchLanguageLocale
        Injector.get().setAppLanguageUseCase().execute(SetAppLanguageUseCase.Param(newLocale, host.requireActivity()))
        return true
    }
}

class OpenOctoPrintMenuItem : MenuItem {
    override val itemId = MENU_ITEM_OPEN_OCTOPRINT
    override val groupId = ""
    override val order = 120
    override val style = MenuItemStyle.Settings
    override val icon = R.drawable.ic_round_open_in_browser_24

    override suspend fun getTitle(context: Context) = context.getString(R.string.main_menu___item_open_octoprint)
    override suspend fun onClicked(host: MenuBottomSheetFragment): Boolean {
        Injector.get().openOctoPrintWebUseCase().execute(host.requireContext())
        return true
    }
}

class NightThemeMenuItem : MenuItem {
    private val isManualDarkModeEnabled get() = Injector.get().octoPreferences().isManualDarkModeEnabled
    override val itemId = MENU_ITEM_NIGHT_THEME
    override val groupId = ""
    override val order = 130
    override val style = MenuItemStyle.Settings
    override val icon = if (isManualDarkModeEnabled) R.drawable.ic_round_light_mode_24 else R.drawable.ic_round_dark_mode_24

    override suspend fun isVisible(destinationId: Int) = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
    override suspend fun getTitle(context: Context) =
        context.getString(if (isManualDarkModeEnabled) R.string.main_menu___item_use_light_mode else R.string.main_menu___item_use_dark_mode)

    override suspend fun onClicked(host: MenuBottomSheetFragment): Boolean {
        Injector.get().octoPreferences().isManualDarkModeEnabled = if (isManualDarkModeEnabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            false
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            true
        }
        return true
    }
}

class PrintNotificationMenuItem : MenuItem {
    private val isPrintNotificationEnabled get() = Injector.get().octoPreferences().isPrintNotificationEnabled
    override val itemId = MENU_ITEM_PRINT_NOTIFICATION
    override val groupId = ""
    override val order = 150
    override val style = MenuItemStyle.Settings
    override val icon = if (isPrintNotificationEnabled) R.drawable.ic_round_notifications_off_24 else R.drawable.ic_round_notifications_active_24

    override suspend fun getTitle(context: Context) = context.getString(
        if (isPrintNotificationEnabled) R.string.main_menu___item_turn_print_notification_off else R.string.main_menu___item_turn_print_notification_on
    )

    override suspend fun onClicked(host: MenuBottomSheetFragment): Boolean {
        Injector.get().octoPreferences().isPrintNotificationEnabled = !isPrintNotificationEnabled

        try {
            if (isPrintNotificationEnabled) {
                Timber.i("Service enabled, starting service")
                host.requireOctoActivity().startPrintNotificationService()
            }
        } catch (e: IllegalStateException) {
            // User might have closed app just in time so we can't start the service
        }

        return true
    }
}

class KeepScreenOnDuringPrintMenuItem : MenuItem {
    private val isKeepScreenOn get() = Injector.get().octoPreferences().isKeepScreenOnDuringPrint
    override val itemId = MENU_ITEM_SCREEN_ON_DURING_PRINT
    override val groupId = ""
    override val order = 160
    override val style = MenuItemStyle.Settings
    override val icon = if (isKeepScreenOn) R.drawable.ic_round_brightness_low_24 else R.drawable.ic_round_brightness_high_24

    override suspend fun getTitle(context: Context) = context.getString(
        if (isKeepScreenOn) R.string.main_menu___item_keep_screen_on_during_pinrt_off else R.string.main_menu___item_keep_screen_on_during_pinrt_on
    )

    override suspend fun onClicked(host: MenuBottomSheetFragment): Boolean {
        Injector.get().octoPreferences().isKeepScreenOnDuringPrint = !isKeepScreenOn
        return true
    }
}

class ChangeOctoPrintInstanceMenuItem : MenuItem {
    override val itemId = MENU_ITEM_CHANGE_OCTOPRINT_INSTANCE
    override val groupId = ""
    override val order = 170
    override val style = MenuItemStyle.Settings
    override val icon = R.drawable.ic_round_swap_horiz_24

    override suspend fun getTitle(context: Context) = context.getString(R.string.main_menu___item_change_octoprint_instance)
    override suspend fun onClicked(host: MenuBottomSheetFragment): Boolean {
        Injector.get().signOutUseCase().execute(Unit)
        return true
    }
}