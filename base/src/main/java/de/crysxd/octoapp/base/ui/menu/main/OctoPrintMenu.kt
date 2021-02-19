package de.crysxd.octoapp.base.ui.menu.main

import android.content.Context
import androidx.core.text.HtmlCompat
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.ui.menu.*
import kotlinx.android.parcel.Parcelize


private val sysCommands get() = Injector.get().octorPrintRepository().getActiveInstanceSnapshot()?.systemCommands

@Parcelize
class OctoPrintMenu : Menu {
    override fun getMenuItem() =
        listOfNotNull(
            listOf(
                OpenOctoPrintMenuItem()
            ),
            sysCommands?.map {
                ExecuteSystemCommandMenuItem(source = it.source, action = it.action)
            }
        ).flatten()

    override fun getTitle(context: Context) = "OctoPrint"
    override fun getSubtitle(context: Context) = context.getString(R.string.main_menu___submenu_subtitle)
    override fun getBottomText(context: Context) = if (sysCommands == null) {
        HtmlCompat.fromHtml(
            "<small><b>Info:</b> Unable to load system commands. Your API key might not have relevant permissions.</small>",
            HtmlCompat.FROM_HTML_MODE_COMPACT
        )
    } else {
        ""
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
    override suspend fun onClicked(host: MenuBottomSheetFragment, executeAsync: SuspendExecutor): Boolean {
        executeAsync {
            Injector.get().openOctoPrintWebUseCase().execute(host.requireContext())
        }
        return true
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
    override fun getConfirmMessage(context: Context) = HtmlCompat.fromHtml(systemCommand?.confirm ?: "Execute?", HtmlCompat.FROM_HTML_MODE_COMPACT)
    override fun getConfirmPositiveAction(context: Context) = systemCommand?.name ?: context.getString(android.R.string.ok)
    override suspend fun onConfirmed(host: MenuBottomSheetFragment, executeAsync: SuspendExecutor): Boolean {
        executeAsync {
            Injector.get().executeSystemCommandUseCase().execute(systemCommand!!)
        }

        return true
    }
}
