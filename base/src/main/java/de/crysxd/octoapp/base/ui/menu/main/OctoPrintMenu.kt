package de.crysxd.octoapp.base.ui.menu.main

import android.content.Context
import androidx.core.text.HtmlCompat
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.UriLibrary
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.ext.open
import de.crysxd.octoapp.base.models.hasCompanionPlugin
import de.crysxd.octoapp.base.ui.menu.ConfirmedMenuItem
import de.crysxd.octoapp.base.ui.menu.Menu
import de.crysxd.octoapp.base.ui.menu.MenuHost
import de.crysxd.octoapp.base.ui.menu.MenuItem
import de.crysxd.octoapp.base.ui.menu.MenuItemStyle
import de.crysxd.octoapp.base.usecase.OpenOctoprintWebUseCase
import kotlinx.parcelize.Parcelize
import java.util.Date
import java.util.concurrent.TimeUnit


private val sysCommands get() = Injector.get().octorPrintRepository().getActiveInstanceSnapshot()?.systemCommands

@Parcelize
class OctoPrintMenu : Menu {

    companion object {
        private const val HIDE_ANNOUNCEMENT_FOR_DAYS = 28L

        fun hasAnnouncement(): Boolean {
            val installed = Injector.get().octorPrintRepository().getActiveInstanceSnapshot().hasCompanionPlugin
            val hiddenAt = Injector.get().octoPreferences().companionAnnouncementHiddenAt ?: Date(0)
            val showNext = Date(hiddenAt.time + TimeUnit.DAYS.toMillis(HIDE_ANNOUNCEMENT_FOR_DAYS))
            val shouldShow = Firebase.remoteConfig.getBoolean("advertise_companion")
            return !installed && Date() > showNext && shouldShow
        }

        fun hideAnnouncement() {
            Injector.get().octoPreferences().companionAnnouncementHiddenAt = Date()
        }
    }

    override suspend fun getMenuItem() =
        listOfNotNull(
            listOf(
                OpenOctoPrintMenuItem(),
                ConfigureRemoteAccessMenuItem(),
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
        title = "Install the companion plugin",
        subtitle = "The OctoApp companion plugin allows push notifications over the internet with better reliability",
        learnMoreButton = "Install the plugin",
        hideButton = "Hide",
        learnMoreUri = UriLibrary.getCompanionPluginUri(),
    ).takeIf { hasAnnouncement() }

    override fun onAnnouncementHidden() {
        super.onAnnouncementHidden()
        hideAnnouncement()
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
        Injector.get().openOctoPrintWebUseCase().execute(OpenOctoprintWebUseCase.Params())
    }
}

class ConfigureRemoteAccessMenuItem : MenuItem {
    override val itemId = MENU_ITEM_CONFIGURE_REMOTE_ACCESS
    override var groupId = ""
    override val order = 201
    override val enforceSingleLine = false
    override val style = MenuItemStyle.OctoPrint
    override val icon = R.drawable.ic_round_cloud_24

    override suspend fun getTitle(context: Context) = context.getString(R.string.main_menu___configure_remote_access)
    override suspend fun onClicked(host: MenuHost?) {
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
        Injector.get().executeSystemCommandUseCase().execute(systemCommand!!)
    }
}
