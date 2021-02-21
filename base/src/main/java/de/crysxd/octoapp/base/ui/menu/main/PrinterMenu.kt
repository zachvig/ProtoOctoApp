package de.crysxd.octoapp.base.ui.menu.main

import android.content.Context
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.ui.common.power.PowerControlsBottomSheet
import de.crysxd.octoapp.base.ui.menu.*
import de.crysxd.octoapp.base.ui.menu.material.MaterialPluginMenu
import de.crysxd.octoapp.base.ui.menu.temperature.TemperatureMenu
import de.crysxd.octoapp.base.ui.widget.webcam.FullscreenWebcamActivity
import de.crysxd.octoapp.base.usecase.CancelPrintJobUseCase
import de.crysxd.octoapp.base.usecase.GetPowerDevicesUseCase
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.flow.first

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
    )

    override fun getTitle(context: Context) = context.getString(R.string.main_menu___menu_printer_title)
    override fun getSubtitle(context: Context) = context.getString(R.string.main_menu___submenu_subtitle)
}


class ShowTemperatureMenuItem : MenuItem {
    override val itemId = MENU_ITEM_TEMPERATURE_MENU
    override var groupId = ""
    override val order = 310
    override val style = MenuItemStyle.Printer
    override val icon = R.drawable.ic_round_local_fire_department_24
    override val showAsSubMenu = true

    override suspend fun isVisible(destinationId: Int) = destinationId != R.id.workspaceConnect
    override suspend fun getTitle(context: Context) = context.getString(R.string.main_menu___item_temperature_presets)
    override suspend fun onClicked(host: MenuBottomSheetFragment, executeAsync: SuspendExecutor): Boolean {
        host.pushMenu(TemperatureMenu())
        return false
    }
}

class ShowMaterialPluginMenuItem : MenuItem {
    override val itemId = MENU_ITEM_MATERIAL_MENU
    override var groupId = ""
    override val order = 320
    override val style = MenuItemStyle.Printer
    override val icon = R.drawable.ic_round_layers_24
    override val showAsSubMenu = true

    override suspend fun isVisible(destinationId: Int) = destinationId != R.id.workspaceConnect
    override suspend fun getTitle(context: Context) = "Materials"
    override suspend fun onClicked(host: MenuBottomSheetFragment, executeAsync: SuspendExecutor): Boolean {
        host.pushMenu(MaterialPluginMenu())
        return false
    }
}

class ShowWebcamMenuItem : MenuItem {
    override val itemId = MENU_ITEM_SHOW_WEBCAM
    override var groupId = ""
    override val order = 330
    override val style = MenuItemStyle.Printer
    override val icon = R.drawable.ic_round_videocam_24

    override suspend fun isVisible(destinationId: Int) = Injector.get().octorPrintRepository().getActiveInstanceSnapshot()?.isWebcamSupported == true
    override suspend fun getTitle(context: Context) = context.getString(R.string.main_menu___show_webcam)
    override suspend fun onClicked(host: MenuBottomSheetFragment, executeAsync: SuspendExecutor): Boolean {
        FullscreenWebcamActivity.start(host.requireActivity())
        return true
    }
}

class OpenPowerControlsMenuItem : MenuItem {
    override val itemId = MENU_ITEM_POWER_CONTROLS
    override var groupId = ""
    override val order = 340
    override val style = MenuItemStyle.Printer
    override val icon = R.drawable.ic_round_power_settings_new_24

    override suspend fun isVisible(destinationId: Int) = Injector.get().getPowerDevicesUseCase().execute(
        GetPowerDevicesUseCase.Params(false)
    ).first().isNotEmpty()

    override suspend fun getTitle(context: Context) = context.getString(R.string.main_menu___item_open_power_controls)
    override suspend fun onClicked(host: MenuBottomSheetFragment, executeAsync: SuspendExecutor): Boolean {
        PowerControlsBottomSheet.createForAction().show(host.parentFragmentManager)
        return true
    }
}

class TurnPsuOffMenuItem : MenuItem {
    override val itemId = MENU_ITEM_TURN_PSU_OFF
    override var groupId = ""
    override val order = 341
    override val style = MenuItemStyle.Printer
    override val icon = R.drawable.ic_baseline_power_off_24

    override suspend fun isVisible(destinationId: Int) = Injector.get().getPowerDevicesUseCase().execute(
        GetPowerDevicesUseCase.Params(false)
    ).first().isNotEmpty() && destinationId == R.id.workspacePrePrint

    override suspend fun getTitle(context: Context) = context.getString(R.string.main_menu___item_turn_psu_off)
    override suspend fun onClicked(host: MenuBottomSheetFragment, executeAsync: SuspendExecutor): Boolean {
        PowerControlsBottomSheet.createForAction(
            PowerControlsBottomSheet.Action.TurnOff,
            PowerControlsBottomSheet.DeviceType.PrinterPsu
        ).show(host.parentFragmentManager)
        return true
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
    override suspend fun onConfirmed(host: MenuBottomSheetFragment, executeAsync: SuspendExecutor): Boolean {
        executeAsync {
            Injector.get().emergencyStopUseCase().execute(Unit)
        }
        return true
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
    override suspend fun onConfirmed(host: MenuBottomSheetFragment, executeAsync: SuspendExecutor): Boolean {
        executeAsync {
            Injector.get().cancelPrintJobUseCase().execute(CancelPrintJobUseCase.Params(restoreTemperatures = true))
        }
        return true
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
    override suspend fun onConfirmed(host: MenuBottomSheetFragment, executeAsync: SuspendExecutor): Boolean {
        executeAsync {
            Injector.get().cancelPrintJobUseCase().execute(CancelPrintJobUseCase.Params(restoreTemperatures = false))
        }
        return true
    }
}