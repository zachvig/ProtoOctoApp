package de.crysxd.baseui.menu.main

import android.content.Context
import de.crysxd.baseui.R
import de.crysxd.baseui.menu.ConfirmedMenuItem
import de.crysxd.baseui.menu.Menu
import de.crysxd.baseui.menu.MenuHost
import de.crysxd.baseui.menu.MenuItem
import de.crysxd.baseui.menu.MenuItemStyle
import de.crysxd.baseui.menu.SubMenuItem
import de.crysxd.baseui.menu.material.MaterialPluginMenu
import de.crysxd.baseui.menu.power.PowerControlsMenu
import de.crysxd.baseui.menu.temperature.TemperatureMenu
import de.crysxd.octoapp.base.UriLibrary
import de.crysxd.octoapp.base.data.models.MenuItems.MENU_ITEM_CANCEL_PRINT
import de.crysxd.octoapp.base.data.models.MenuItems.MENU_ITEM_CANCEL_PRINT_KEEP_TEMPS
import de.crysxd.octoapp.base.data.models.MenuItems.MENU_ITEM_COOL_DOWN
import de.crysxd.octoapp.base.data.models.MenuItems.MENU_ITEM_EMERGENCY_STOP
import de.crysxd.octoapp.base.data.models.MenuItems.MENU_ITEM_MATERIAL_MENU
import de.crysxd.octoapp.base.data.models.MenuItems.MENU_ITEM_OPEN_TERMINAL
import de.crysxd.octoapp.base.data.models.MenuItems.MENU_ITEM_POWER_CONTROLS
import de.crysxd.octoapp.base.data.models.MenuItems.MENU_ITEM_SHOW_WEBCAM
import de.crysxd.octoapp.base.data.models.MenuItems.MENU_ITEM_TEMPERATURE_MENU
import de.crysxd.octoapp.base.data.models.MenuItems.MENU_ITEM_TURN_PSU_OFF
import de.crysxd.octoapp.base.di.BaseInjector
import de.crysxd.octoapp.base.ext.open
import de.crysxd.octoapp.base.usecase.BaseChangeTemperaturesUseCase
import de.crysxd.octoapp.base.usecase.CancelPrintJobUseCase
import de.crysxd.octoapp.octoprint.plugins.power.PowerDevice
import kotlinx.coroutines.runBlocking
import kotlinx.parcelize.Parcelize

@Parcelize
class PrinterMenu : Menu {
    override suspend fun getMenuItem() = listOf(
        CancelPrintMenuItem(),
        CancelPrintKeepTemperaturesMenuItem(),
        EmergencyStopMenuItem(),
        ShowTemperatureMenuItem(),
        ShowWebcamMenuItem(),
        TurnPsuOffMenuItem(),
        OpenPowerControlsMenuItem(),
        ShowMaterialPluginMenuItem(),
        OpenTerminalMenuItem(),
        CoolDownMenuItem(),
    )

    override suspend fun getTitle(context: Context) = context.getString(R.string.main_menu___menu_printer_title)
    override suspend fun getSubtitle(context: Context) = context.getString(R.string.main_menu___submenu_subtitle)
}


class ShowTemperatureMenuItem : SubMenuItem() {
    override val itemId = MENU_ITEM_TEMPERATURE_MENU
    override var groupId = ""
    override val order = 310
    override val style = MenuItemStyle.Printer
    override val icon = R.drawable.ic_round_local_fire_department_24
    override val subMenu: Menu get() = TemperatureMenu()
    override fun isVisible(destinationId: Int) = destinationId != R.id.workspaceConnect
    override fun getTitle(context: Context) = context.getString(R.string.main_menu___item_temperature_presets)
}

class ShowMaterialPluginMenuItem : SubMenuItem() {
    override val itemId = MENU_ITEM_MATERIAL_MENU
    override var groupId = ""
    override val order = 320
    override val style = MenuItemStyle.Printer
    override val icon = R.drawable.ic_round_layers_24
    override val showAsSubMenu = true
    override val subMenu: Menu
        get() = MaterialPluginMenu()

    override fun isVisible(destinationId: Int) = destinationId != R.id.workspaceConnect
    override fun getTitle(context: Context) = context.getString(R.string.main_menu___item_materials)
}

class ShowWebcamMenuItem : MenuItem {
    override val itemId = MENU_ITEM_SHOW_WEBCAM
    override var groupId = ""
    override val order = 340
    override val style = MenuItemStyle.Printer
    override val icon = R.drawable.ic_round_videocam_24

    override fun isVisible(destinationId: Int) = BaseInjector.get().octorPrintRepository().getActiveInstanceSnapshot()?.isWebcamSupported == true
    override fun getTitle(context: Context) = context.getString(R.string.main_menu___show_webcam)
    override suspend fun onClicked(host: MenuHost?) {
        host?.getMenuActivity()?.let {
            UriLibrary.getWebcamUri().open(it)
        }
        host?.closeMenu()
    }
}

class OpenPowerControlsMenuItem : SubMenuItem() {
    override val itemId = MENU_ITEM_POWER_CONTROLS
    override var groupId = ""
    override val order = 330
    override val style = MenuItemStyle.Printer
    override val icon = R.drawable.ic_round_power_settings_new_24
    override val subMenu = PowerControlsMenu()
    override fun getTitle(context: Context) = context.getString(R.string.main_menu___item_open_power_controls)
}

class TurnPsuOffMenuItem : MenuItem {
    override val itemId = MENU_ITEM_TURN_PSU_OFF
    override var groupId = ""
    override val order = 331
    override val style = MenuItemStyle.Printer
    override val icon = R.drawable.ic_round_power_off_24

    override fun isVisible(destinationId: Int): Boolean = runBlocking {
        // Run blocking is not ideal...but we don't have any network requests here (not like before.....)

        if (destinationId != R.id.workspacePrint) {
            return@runBlocking false
        }

        val settings = BaseInjector.get().octorPrintRepository().getActiveInstanceSnapshot()?.settings ?: return@runBlocking false
        val devices = BaseInjector.get().octoPrintProvider().octoPrint().createPowerPluginsCollection().getDevices(settings)
        return@runBlocking devices.any { it.capabilities.contains(PowerDevice.Capability.ControlPrinterPower) }
    }

    override fun getTitle(context: Context) = context.getString(R.string.main_menu___item_turn_psu_off)
    override suspend fun onClicked(host: MenuHost?) {
        host?.pushMenu(PowerControlsMenu(PowerControlsMenu.DeviceType.PrinterPsu, PowerControlsMenu.Action.TurnOff))
    }
}

class OpenTerminalMenuItem : MenuItem {
    override val itemId = MENU_ITEM_OPEN_TERMINAL
    override var groupId = ""
    override val order = 332
    override val style = MenuItemStyle.Printer
    override val icon = R.drawable.ic_round_code_24

    override fun isVisible(destinationId: Int) = destinationId == R.id.workspacePrint || destinationId == R.id.workspacePrePrint
    override fun getTitle(context: Context) = context.getString(R.string.main_menu___item_open_terminal)
    override suspend fun onClicked(host: MenuHost?) {
        host?.getNavController()?.navigate(R.id.action_open_terminal)
        host?.closeMenu()
    }
}

class EmergencyStopMenuItem : ConfirmedMenuItem() {
    override val itemId = MENU_ITEM_EMERGENCY_STOP
    override var groupId = ""
    override val order = 350
    override val style = MenuItemStyle.Printer

    override val icon = R.drawable.ic_round_offline_bolt_24
    override fun getConfirmMessage(context: Context) = context.getString(R.string.emergency_stop_confirmation_message)
    override fun getConfirmPositiveAction(context: Context) = context.getString(R.string.emergency_stop_confirmation_action)
    override fun isVisible(destinationId: Int) = destinationId == R.id.workspacePrint
    override fun getTitle(context: Context) = context.getString(R.string.main_menu___item_emergency_stop)
    override suspend fun onConfirmed(host: MenuHost?) {
        BaseInjector.get().emergencyStopUseCase().execute(Unit)
    }
}

class CancelPrintKeepTemperaturesMenuItem : ConfirmedMenuItem() {
    override val itemId = MENU_ITEM_CANCEL_PRINT_KEEP_TEMPS
    override var groupId = ""
    override val order = 351
    override val style = MenuItemStyle.Printer
    override val icon = R.drawable.ic_round_stop_24

    override fun getConfirmMessage(context: Context) = context.getString(R.string.cancel_print_confirmation_message)
    override fun getConfirmPositiveAction(context: Context) = context.getString(R.string.cancel_print_confirmation_action)
    override fun isVisible(destinationId: Int) = destinationId == R.id.workspacePrint
    override fun getTitle(context: Context) = context.getString(R.string.main_menu___item_cancel_print_keep_temp)
    override suspend fun onConfirmed(host: MenuHost?) {
        BaseInjector.get().cancelPrintJobUseCase().execute(CancelPrintJobUseCase.Params(restoreTemperatures = true))
    }
}

class CancelPrintMenuItem : ConfirmedMenuItem() {
    override val itemId = MENU_ITEM_CANCEL_PRINT
    override var groupId = ""
    override val order = 352
    override val style = MenuItemStyle.Printer
    override val icon = R.drawable.ic_round_stop_24

    override fun getConfirmMessage(context: Context) = context.getString(R.string.cancel_print_confirmation_message)
    override fun getConfirmPositiveAction(context: Context) = context.getString(R.string.cancel_print_confirmation_action)
    override fun isVisible(destinationId: Int) = destinationId == R.id.workspacePrint
    override fun getTitle(context: Context) = context.getString(R.string.main_menu___item_cancel_print)
    override suspend fun onConfirmed(host: MenuHost?) {
        BaseInjector.get().cancelPrintJobUseCase().execute(CancelPrintJobUseCase.Params(restoreTemperatures = false))
    }
}

class CoolDownMenuItem : MenuItem {
    override val itemId = MENU_ITEM_COOL_DOWN
    override var groupId = ""
    override val order = 333
    override val style = MenuItemStyle.Printer
    override val icon = R.drawable.ic_round_ac_unit_24

    override fun isVisible(destinationId: Int) = destinationId == R.id.workspacePrePrint
    override fun getTitle(context: Context) = context.getString(R.string.main_menu___item_cool_down)
    override suspend fun onClicked(host: MenuHost?) {
        BaseInjector.get().setTargetTemperatureUseCase().execute(
            BaseChangeTemperaturesUseCase.Params(
                listOf(
                    BaseChangeTemperaturesUseCase.Temperature(component = "tool0", temperature = 0),
                    BaseChangeTemperaturesUseCase.Temperature(component = "tool1", temperature = 0),
                    BaseChangeTemperaturesUseCase.Temperature(component = "tool2", temperature = 0),
                    BaseChangeTemperaturesUseCase.Temperature(component = "tool3", temperature = 0),
                    BaseChangeTemperaturesUseCase.Temperature(component = "bed", temperature = 0),
                    BaseChangeTemperaturesUseCase.Temperature(component = "chamber", temperature = 0),
                )
            )
        )
    }
}