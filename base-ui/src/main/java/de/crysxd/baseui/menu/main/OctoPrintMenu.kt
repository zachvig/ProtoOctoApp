package de.crysxd.baseui.menu.main

import android.content.Context
import androidx.core.text.HtmlCompat
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import de.crysxd.baseui.R
import de.crysxd.baseui.menu.ConfirmedMenuItem
import de.crysxd.baseui.menu.Menu
import de.crysxd.baseui.menu.MenuHost
import de.crysxd.baseui.menu.MenuItem
import de.crysxd.baseui.menu.MenuItemStyle
import de.crysxd.octoapp.base.UriLibrary
import de.crysxd.octoapp.base.data.models.MenuItems.MENU_EXECUTE_SYSTEM_COMMAND
import de.crysxd.octoapp.base.data.models.MenuItems.MENU_ITEM_CONFIGURE_REMOTE_ACCESS
import de.crysxd.octoapp.base.data.models.MenuItems.MENU_ITEM_OPEN_OCTOPRINT
import de.crysxd.octoapp.base.data.models.MenuItems.MENU_ITEM_SHOW_FILES
import de.crysxd.octoapp.base.data.models.hasPlugin
import de.crysxd.octoapp.base.di.BaseInjector
import de.crysxd.octoapp.base.ext.open
import de.crysxd.octoapp.base.usecase.OpenOctoprintWebUseCase
import de.crysxd.octoapp.octoprint.isOctoEverywhereUrl
import de.crysxd.octoapp.octoprint.models.settings.Settings
import kotlinx.parcelize.Parcelize
import java.util.Date
import java.util.concurrent.TimeUnit


private val sysCommands get() = BaseInjector.get().octorPrintRepository().getActiveInstanceSnapshot()?.systemCommands

@Parcelize
class OctoPrintMenu : Menu {

    companion object {
        private const val HIDE_ANNOUNCEMENT_FOR_DAYS = 14L

        fun shouldAnnounceOctoEverywhere(): Boolean {
            val instance = BaseInjector.get().octorPrintRepository().getActiveInstanceSnapshot()
            val oeInstalled = instance.hasPlugin(Settings.OctoEverywhere::class)
            val oeConfigured = instance?.alternativeWebUrl?.isOctoEverywhereUrl() == true || instance?.webUrl?.isOctoEverywhereUrl() == true
            val hiddenAt = BaseInjector.get().octoPreferences().octoEverywhereAnnouncementHiddenAt ?: Date(0)
            val showNext = Date(hiddenAt.time + TimeUnit.DAYS.toMillis(HIDE_ANNOUNCEMENT_FOR_DAYS))
            val shouldShow = Firebase.remoteConfig.getBoolean("advertise_octoeverywhere")
            return oeInstalled && Date() > showNext && !oeConfigured && shouldShow
        }

        fun shouldAnnounceCompanion(): Boolean {
            val instance = BaseInjector.get().octorPrintRepository().getActiveInstanceSnapshot()
            val companionInstalled = instance.hasPlugin(Settings.OctoAppCompanionSettings::class)
            val hiddenAt = BaseInjector.get().octoPreferences().companionAnnouncementHiddenAt ?: Date(0)
            val showNext = Date(hiddenAt.time + TimeUnit.DAYS.toMillis(HIDE_ANNOUNCEMENT_FOR_DAYS))
            val shouldShow = Firebase.remoteConfig.getBoolean("advertise_companion")
            return !companionInstalled && Date() > showNext && shouldShow
        }

        fun getAnnouncementCounter(): Int {
            var counter = 0
            if (shouldAnnounceCompanion()) counter++
            if (shouldAnnounceOctoEverywhere()) counter++
            return counter
        }

        fun hideCompanionAnnouncement() {
            BaseInjector.get().octoPreferences().companionAnnouncementHiddenAt = Date()
        }

        fun hideOctoEverywhereAnnouncement() {
            BaseInjector.get().octoPreferences().octoEverywhereAnnouncementHiddenAt = Date()
        }
    }

    override suspend fun getMenuItem() =
        listOfNotNull(
            listOf(
                OpenOctoPrintMenuItem(),
                ConfigureRemoteAccessMenuItem(suppressBadge = false),
                ShowFilesMenuItem(),
            ),
            sysCommands?.map {
                ExecuteSystemCommandMenuItem(source = it.source ?: "Unknown", action = it.action ?: "Unknown")
            }
        ).flatten()

    override suspend fun getTitle(context: Context) = "OctoPrint"
    override suspend fun getSubtitle(context: Context) = context.getString(R.string.main_menu___submenu_subtitle)
    override fun getBottomText(context: Context) = if (sysCommands == null) {
        HtmlCompat.fromHtml(
            "<small><b>Info:</b> Unable to load system commands. Your API key might not have relevant permissions.</small>",
            HtmlCompat.FROM_HTML_MODE_COMPACT
        )
    } else {
        ""
    }

    override suspend fun getAnnouncement(context: Context) = Menu.Announcement(
        title = context.getString(R.string.main_menu___companion_plugin_announcement___title),
        subtitle = context.getString(R.string.main_menu___companion_plugin_announcement___subtitle),
        learnMoreButton = context.getString(R.string.main_menu___companion_plugin_announcement___install),
        hideButton = context.getString(R.string.hide),
        learnMoreUri = UriLibrary.getCompanionPluginUri(),
    ).takeIf { shouldAnnounceCompanion() }

    override fun onAnnouncementHidden() {
        super.onAnnouncementHidden()
        hideCompanionAnnouncement()
    }
}

class OpenOctoPrintMenuItem : MenuItem {
    override val itemId = MENU_ITEM_OPEN_OCTOPRINT
    override var groupId = ""
    override val order = 200
    override val enforceSingleLine = false
    override val style = MenuItemStyle.OctoPrint
    override val icon = R.drawable.ic_round_open_in_browser_24

    override suspend fun getTitle(context: Context) = context.getString(R.string.main_menu___item_open_octoprint)
    override suspend fun onClicked(host: MenuHost?) {
        BaseInjector.get().openOctoPrintWebUseCase().execute(OpenOctoprintWebUseCase.Params())
    }
}

class ConfigureRemoteAccessMenuItem(val suppressBadge: Boolean = true) : MenuItem {
    override val itemId = MENU_ITEM_CONFIGURE_REMOTE_ACCESS
    override var groupId = ""
    override val order = 201
    override val enforceSingleLine = false
    override val style = MenuItemStyle.OctoPrint
    override val icon = R.drawable.ic_round_cloud_24

    override suspend fun getBadgeCount() = if (OctoPrintMenu.shouldAnnounceOctoEverywhere() && !suppressBadge) 1 else 0
    override suspend fun getTitle(context: Context) = context.getString(R.string.main_menu___configure_remote_access)
    override suspend fun onClicked(host: MenuHost?) {
        OctoPrintMenu.hideOctoEverywhereAnnouncement()
        host?.getMenuActivity()?.let {
            UriLibrary.getConfigureRemoteAccessUri().open(it)
        }
        host?.closeMenu()
    }
}

class ShowFilesMenuItem : MenuItem {
    override val itemId = MENU_ITEM_SHOW_FILES
    override var groupId = ""
    override val order = 202
    override val enforceSingleLine = false
    override val style = MenuItemStyle.OctoPrint
    override val icon = R.drawable.ic_round_folder_24

    override suspend fun getTitle(context: Context) = context.getString(R.string.main_menu___show_files)
    override suspend fun onClicked(host: MenuHost?) {
        host?.getMenuActivity()?.let {
            UriLibrary.getFileManagerUri().open(it)
        }
        host?.closeMenu()
    }
}


class ExecuteSystemCommandMenuItem(val source: String, val action: String) : ConfirmedMenuItem() {
    companion object {
        fun forItemId(itemId: String) = itemId.split("/").let {
            ExecuteSystemCommandMenuItem(it[1], it[2])
        }
    }

    override val itemId = "$MENU_EXECUTE_SYSTEM_COMMAND/$source/$action"
    override var groupId = ""
    override val order = 210
    override val style = MenuItemStyle.OctoPrint
    override val icon = when (systemCommand?.action) {
        "reboot" -> R.drawable.ic_command_restart_24px
        "restart" -> R.drawable.ic_command_restart_24px
        "restart_safe" -> R.drawable.ic_command_restart_24px
        "shutdown" -> R.drawable.ic_command_shutdown_24px
        else -> R.drawable.ic_command_generic_24px
    }

    private val systemCommand
        get() = sysCommands?.firstOrNull { it.source == source && it.action == action }

    override suspend fun isVisible(destinationId: Int) = systemCommand != null
    override suspend fun getTitle(context: Context) = systemCommand?.name ?: "Unknown system command"
    override fun getConfirmMessage(context: Context) = systemCommand?.confirm ?: "Execute?"
    override fun getConfirmPositiveAction(context: Context) = systemCommand?.name ?: context.getString(android.R.string.ok)
    override suspend fun onConfirmed(host: MenuHost?) {
        BaseInjector.get().executeSystemCommandUseCase().execute(systemCommand!!)
    }
}