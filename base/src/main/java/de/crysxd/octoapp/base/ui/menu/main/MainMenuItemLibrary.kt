package de.crysxd.octoapp.base.ui.menu.main

import de.crysxd.octoapp.base.ui.menu.MenuItem
import de.crysxd.octoapp.base.ui.menu.material.MaterialPluginMenu
import de.crysxd.octoapp.base.ui.menu.power.PowerControlsMenu
import de.crysxd.octoapp.base.ui.menu.switchprinter.AddInstanceMenuItem
import de.crysxd.octoapp.base.ui.menu.switchprinter.SwitchInstanceMenuItem
import de.crysxd.octoapp.base.ui.menu.temperature.ApplyTemperaturePresetMenuItem
import de.crysxd.octoapp.base.ui.menu.webcam.WebcamSettingsMenu

const val MENU_ITEM_SUPPORT_OCTOAPP = "main___support_octoapp"
const val MENU_ITEM_SETTINGS_MENU = "main___settings_menu"
const val MENU_ITEM_PRINTER_MENU = "main___printer_menu"
const val MENU_ITEM_NEWS = "main___news"
const val MENU_ITEM_OCTOPRINT = "main___octoprint"
const val MENU_ITEM_HELP = "settings___help"
const val MENU_ITEM_CRASH_REPORTING = "privacy___crash_reporting"
const val MENU_ITEM_ANALYTICS = "privacy___analytics"
const val MENU_ITEM_CHANGE_LANGUAGE = "settings___change_language"
const val MENU_ITEM_OPEN_OCTOPRINT = "settings___open_octoprint"
const val MENU_ITEM_CONFIGURE_REMOTE_ACCESS = "settings___configure_remote_access"
const val MENU_ITEM_SHOW_FILES = "octoprint___show_files"
const val MENU_EXECUTE_SYSTEM_COMMAND = "octoprint___execute_system_command"
const val MENU_ITEM_NIGHT_THEME = "settings___night_theme"
const val MENU_ITEM_CHANGE_OCTOPRINT_INSTANCE = "settings___change_octoprint_instnace"
const val MENU_ITEM_AUTOMATIC_LIGHTS = "settings___automatic_lights"
const val MENU_ITEM_SHOW_OCTOAPP_LAB = "settings___open_octoapp_lab"
const val MENU_ITEM_CUSTOMIZE_WIDGETS = "settings___customize_widgets"
const val MENU_ITEM_PRINT_NOTIFICATION = "settings___print_notification"
const val MENU_ITEM_SCREEN_ON_DURING_PRINT = "settings___keep_screen_on_during__print"
const val MENU_ITEM_AUTO_CONNECT_PRINTER = "settings___auto_connect_printer"
const val MENU_ITEM_CANCEL_PRINT = "printer___cancel_print"
const val MENU_ITEM_OPEN_TERMINAL = "printer___open_terminal"
const val MENU_ITEM_CANCEL_PRINT_KEEP_TEMPS = "printer___cancel_print_keep_temps"
const val MENU_ITEM_SHOW_WEBCAM = "printer___show_webcam"
const val MENU_ITEM_EMERGENCY_STOP = "printer___cemergency_stop"
const val MENU_ITEM_TURN_PSU_OFF = "printer___turn_psu_off"
const val MENU_ITEM_POWER_CONTROLS = "printer___open_power_controls"
const val MENU_ITEM_SIGN_OUT = "switch___sign_out"
const val MENU_ITEM_SWITCH_INSTANCE = "switch___to_instance/"
const val MENU_ITEM_ADD_INSTANCE = "switch___add_instance"
const val MENU_ITEM_ENABLE_QUICK_SWITCH = "switch___enable_quick_switch"
const val MENU_ITEM_APPLY_TEMPERATURE_PRESET = "temp___apply_temperature_preset/"
const val MENU_ITEM_TEMPERATURE_MENU = "printer___temp_menu"
const val MENU_ITEM_MATERIAL_MENU = "printer___material_menu"
const val MENU_ITEM_ACTIVATE_MATERIAL = "material___activate_material"
const val MENU_ITEM_SHOW_POWER_DEVICE_ACTIONS = "power___show_device_actions"
const val MENU_ITEM_POWER_DEVICE_OFF = "power___device_off"
const val MENU_ITEM_POWER_DEVICE_ON = "power___device_on"
const val MENU_ITEM_POWER_DEVICE_CYCLE = "power___cycle"
const val MENU_ITEM_ROTATE_APP = "lab___rotate"
const val MENU_ITEM_SHOW_WEBCAM_RESOLUTION = "webcam___show_resolution"
const val MENU_ITEM_WEBCAM_ASPECT_RATIO_SOURCE = "webcam___aspect_ratio_source"
const val MENU_ITEM_ENABLE_FULL_WEBCAM_RESOLUTION = "webcam___enable_full_resolution"


class MenuItemLibrary {

    private val map = mapOf(
        MENU_ITEM_SUPPORT_OCTOAPP to SupportOctoAppMenuItem::class,
        MENU_ITEM_SETTINGS_MENU to ShowSettingsMenuItem::class,
        MENU_ITEM_PRINTER_MENU to ShowPrinterMenuItem::class,
        MENU_ITEM_NEWS to ShowNewsMenuItem::class,
        MENU_ITEM_CHANGE_LANGUAGE to ChangeLanguageMenuItem::class,
        MENU_ITEM_OPEN_OCTOPRINT to OpenOctoPrintMenuItem::class,
        MENU_ITEM_CHANGE_OCTOPRINT_INSTANCE to ChangeOctoPrintInstanceMenuItem::class,
        MENU_ITEM_CANCEL_PRINT to CancelPrintMenuItem::class,
        MENU_ITEM_EMERGENCY_STOP to EmergencyStopMenuItem::class,
        MENU_ITEM_TURN_PSU_OFF to TurnPsuOffMenuItem::class,
        MENU_ITEM_POWER_CONTROLS to OpenPowerControlsMenuItem::class,
        MENU_ITEM_NIGHT_THEME to NightThemeMenuItem::class,
        MENU_ITEM_PRINT_NOTIFICATION to PrintNotificationMenuItem::class,
        MENU_ITEM_SCREEN_ON_DURING_PRINT to KeepScreenOnDuringPrintMenuItem::class,
        MENU_ITEM_ADD_INSTANCE to AddInstanceMenuItem::class,
        MENU_ITEM_SHOW_WEBCAM to ShowWebcamMenuItem::class,
        MENU_ITEM_CANCEL_PRINT_KEEP_TEMPS to CancelPrintKeepTemperaturesMenuItem::class,
        MENU_ITEM_TEMPERATURE_MENU to ShowTemperatureMenuItem::class,
        MENU_ITEM_AUTO_CONNECT_PRINTER to AutoConnectPrinterMenuItem::class,
        MENU_ITEM_MATERIAL_MENU to ShowMaterialPluginMenuItem::class,
        MENU_ITEM_HELP to HelpMenuItem::class,
        MENU_ITEM_CUSTOMIZE_WIDGETS to CustomizeWidgetsMenuItem::class,
        MENU_ITEM_OPEN_TERMINAL to OpenTerminalMenuItem::class,
        MENU_ITEM_SHOW_OCTOAPP_LAB to ShowOctoAppLabMenuItem::class,
        MENU_ITEM_ROTATE_APP to OctoAppLabMenu.RotationMenuItem::class,
        MENU_ITEM_CONFIGURE_REMOTE_ACCESS to ConfigureRemoteAccessMenuItem::class,
        MENU_ITEM_SHOW_FILES to ShowFilesMenuItem::class,
        MENU_ITEM_AUTOMATIC_LIGHTS to AutomaticLightsSettingsMenuItem::class,
        MENU_ITEM_SHOW_WEBCAM_RESOLUTION to WebcamSettingsMenu.ShowResolutionMenuItem::class,
        MENU_ITEM_ENABLE_FULL_WEBCAM_RESOLUTION to WebcamSettingsMenu.EnableFullResolutionMenuItem::class,
        MENU_ITEM_WEBCAM_ASPECT_RATIO_SOURCE to WebcamSettingsMenu.AspectRatioMenuItem::class,
    )

    operator fun get(itemId: String): MenuItem? = when {
        map.containsKey(itemId) -> map[itemId]?.java?.constructors?.firstOrNull()?.newInstance() as? MenuItem
        itemId.startsWith(MENU_ITEM_SWITCH_INSTANCE) -> SwitchInstanceMenuItem.forItemId(itemId)
        itemId.startsWith(MENU_ITEM_APPLY_TEMPERATURE_PRESET) -> ApplyTemperaturePresetMenuItem.forItemId(itemId)
        itemId.startsWith(MENU_EXECUTE_SYSTEM_COMMAND) -> ExecuteSystemCommandMenuItem.forItemId(itemId)
        itemId.startsWith(MENU_ITEM_ACTIVATE_MATERIAL) -> MaterialPluginMenu.ActivateMaterialMenuItem.forItemId(itemId)
        itemId.startsWith(MENU_ITEM_SHOW_POWER_DEVICE_ACTIONS) -> PowerControlsMenu.ShowPowerDeviceActionsMenuItem.forItemId(itemId)
        itemId.startsWith(MENU_ITEM_POWER_DEVICE_OFF) -> PowerControlsMenu.TurnPowerDeviceOffMenuItem.forItemId(itemId)
        itemId.startsWith(MENU_ITEM_POWER_DEVICE_ON) -> PowerControlsMenu.TurnPowerDeviceOnMenuItem.forItemId(itemId)
        itemId.startsWith(MENU_ITEM_POWER_DEVICE_CYCLE) -> PowerControlsMenu.CyclePowerDeviceMenuItem.forItemId(itemId)
        else -> null
    }
}