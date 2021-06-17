package de.crysxd.octoapp.base.ui.menu.main

import android.content.Context
import androidx.navigation.fragment.findNavController
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.UriLibrary
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.ext.open
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.base.ui.menu.*
import de.crysxd.octoapp.base.ui.menu.material.MaterialPluginMenu
import de.crysxd.octoapp.base.ui.menu.power.PowerControlsMenu
import de.crysxd.octoapp.base.ui.menu.temperature.TemperatureMenu
import de.crysxd.octoapp.base.usecase.CancelPrintJobUseCase
import de.crysxd.octoapp.base.usecase.GetPowerDevicesUseCase
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
    override suspend fun isVisible(destinationId: Int) = destinationId != R.id.workspaceConnect
    override suspend fun getTitle(context: Context) = context.getString(R.string.main_menu___item_temperature_presets)
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

    override suspend fun isVisible(destinationId: Int) = destinationId != R.id.workspaceConnect
    override suspend fun getTitle(context: Context) = context.getString(R.string.main_menu___item_materials)
}

class ShowWebcamMenuItem : MenuItem {
    override val itemId = MENU_ITEM_SHOW_WEBCAM
    override var groupId = ""
    override val order = 340
    override val style = MenuItemStyle.Printer
    override val icon = R.drawable.ic_round_videocam_24

    override suspend fun isVisible(destinationId: Int) = Injector.get().octorPrintRepository().getActiveInstanceSnapshot()?.isWebcamSupported == true
    override suspend fun getTitle(context: Context) = context.getString(R.string.main_menu___show_webcam)
    override suspend fun onClicked(host: MenuBottomSheetFragment?) {
        host?.requireOctoActivity()?.let {
            UriLibrary.getWebcamUri().open(it)
        }
        host?.dismissAllowingStateLoss()
    }
}

class OpenPowerControlsMenuItem : SubMenuItem() {
    override val itemId = MENU_ITEM_POWER_CONTROLS
    override var groupId = ""
    override val order = 330
    override val style = MenuItemStyle.Printer
    override val icon = R.drawable.ic_round_power_settings_new_24
    override val subMenu = PowerControlsMenu()
    override suspend fun getTitle(context: Context) = context.getString(R.string.main_menu___item_open_power_controls)
}

class TurnPsuOffMenuItem : MenuItem {
    override val itemId = MENU_ITEM_TURN_PSU_OFF
    override var groupId = ""
    override val order = 331
    override val style = MenuItemStyle.Printer
    override val icon = R.drawable.ic_round_power_off_24

    override suspend fun isVisible(destinationId: Int) = Injector.get().getPowerDevicesUseCase().execute(
        GetPowerDevicesUseCase.Params(false)
    ).isNotEmpty() && destinationId == R.id.workspacePrePrint

    override suspend fun getTitle(context: Context) = context.getString(R.string.main_menu___item_turn_psu_off)
    override suspend fun onClicked(host: MenuBottomSheetFragment?) {
        host?.pushMenu(PowerControlsMenu(PowerControlsMenu.DeviceType.PrinterPsu, PowerControlsMenu.Action.TurnOff))
    }
}

class OpenTerminalMenuItem : MenuItem {
    override val itemId = MENU_ITEM_OPEN_TERMINAL
    override var groupId = ""
    override val order = 332
    override val style = MenuItemStyle.Printer
    override val icon = R.drawable.ic_round_code_24

    override suspend fun isVisible(destinationId: Int) = destinationId == R.id.workspacePrint || destinationId == R.id.workspacePrePrint
    override suspend fun getTitle(context: Context) = context.getString(R.string.main_menu___item_open_terminal)
    override suspend fun onClicked(host: MenuBottomSheetFragment?) {
        host?.findNavController()?.navigate(R.id.action_open_terminal)
        host?.dismissAllowingStateLoss()
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
    override suspend fun isVisible(destinationId: Int) = destinationId == R.id.workspacePrint
    override suspend fun getTitle(context: Context) = context.getString(R.string.main_menu___item_emergency_stop)
    override suspend fun onConfirmed(host: MenuBottomSheetFragment) {
        Injector.get().emergencyStopUseCase().execute(Unit)
    }
}

class CancelPrintKeepTemperaturesMenuItem : ConfirmedMenuItem() {
    override val itemId = MENU_ITEM_CANCEL_PRINT_KEEP_TEMPS
    override var groupId = ""
    override val order = 351
    override val style = MenuItemStyle.Printer
    override val icon = R.drawable.ic_round_cancel_24

    override fun getConfirmMessage(context: Context) = context.getString(R.string.cancel_print_confirmation_message)
    override fun getConfirmPositiveAction(context: Context) = context.getString(R.string.cancel_print_confirmation_action)
    override suspend fun isVisible(destinationId: Int) = destinationId == R.id.workspacePrint
    override suspend fun getTitle(context: Context) = context.getString(R.string.main_menu___item_cancel_print_keep_temp)
    override suspend fun onConfirmed(host: MenuBottomSheetFragment) {
        Injector.get().cancelPrintJobUseCase().execute(CancelPrintJobUseCase.Params(restoreTemperatures = true))
    }
}

class CancelPrintMenuItem : ConfirmedMenuItem() {
    override val itemId = MENU_ITEM_CANCEL_PRINT
    override var groupId = ""
    override val order = 352
    override val style = MenuItemStyle.Printer
    override val icon = R.drawable.ic_round_cancel_24

    override fun getConfirmMessage(context: Context) = context.getString(R.string.cancel_print_confirmation_message)
    override fun getConfirmPositiveAction(context: Context) = context.getString(R.string.cancel_print_confirmation_action)
    override suspend fun isVisible(destinationId: Int) = destinationId == R.id.workspacePrint
    override suspend fun getTitle(context: Context) = context.getString(R.string.main_menu___item_cancel_print)
    override suspend fun onConfirmed(host: MenuBottomSheetFragment) {
        Injector.get().cancelPrintJobUseCase().execute(CancelPrintJobUseCase.Params(restoreTemperatures = false))
    }
}