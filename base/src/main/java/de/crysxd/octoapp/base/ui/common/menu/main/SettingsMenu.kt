package de.crysxd.octoapp.base.ui.common.menu.main

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.feedback.SendFeedbackDialog
import de.crysxd.octoapp.base.ui.common.menu.Menu
import de.crysxd.octoapp.base.ui.common.menu.MenuBottomSheetFragment
import de.crysxd.octoapp.base.ui.common.menu.MenuItem
import de.crysxd.octoapp.base.ui.common.menu.MenuItemStyle
import de.crysxd.octoapp.base.usecase.SetAppLanguageUseCase

class SettingsMenu : Menu {
    override fun getMenuItem() = listOf(
        SendFeedbackMenuItem(),
        ChangeLanguageMenuItem(),
        OpenOctoPrintMenuItem(),
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

class ChangeOctoPrintInstanceMenuItem : MenuItem {
    override val itemId = MENU_ITEM_CHANGE_OCTOPRINT_INSTANCE
    override val groupId = ""
    override val order = 130
    override val style = MenuItemStyle.Settings
    override val icon = R.drawable.ic_round_swap_horiz_24

    override suspend fun getTitle(context: Context) = context.getString(R.string.main_menu___item_change_octoprint_instance)
    override suspend fun onClicked(host: MenuBottomSheetFragment): Boolean {
        Injector.get().signOutUseCase().execute(Unit)
        return true
    }
}