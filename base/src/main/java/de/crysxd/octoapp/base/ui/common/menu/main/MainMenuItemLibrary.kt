package de.crysxd.octoapp.base.ui.common.menu.main

import de.crysxd.octoapp.base.ui.common.menu.MenuItem
import de.crysxd.octoapp.base.ui.common.menu.switchprinter.AddInstanceMenuItem
import de.crysxd.octoapp.base.ui.common.menu.switchprinter.SwitchInstanceMenuItem
import de.crysxd.octoapp.base.ui.common.menu.temperature.ApplyTemperaturePresetMenuItem

const val MENU_ITEM_SUPPORT_OCTOAPP = "main___support_octoapp"
const val MENU_ITEM_SETTINGS_MENU = "main___settings_menu"
const val MENU_ITEM_PRINTER_MENU = "main___printer_menu"
const val MENU_ITEM_NEWS = "main___news"
const val MENU_ITEM_OCTOPRINT = "main___octoprint"
const val MENU_ITEM_SEND_FEEDBACK = "settings___send_feedback"
const val MENU_ITEM_CHANGE_LANGUAGE = "settings___change_language"
const val MENU_ITEM_OPEN_OCTOPRINT = "settings___open_octoprint"
const val MENU_EXECUTE_SYSTEM_COMMAND = "octoprint___execute_system_command"
const val MENU_ITEM_NIGHT_THEME = "settings___night_theme"
const val MENU_ITEM_CHANGE_OCTOPRINT_INSTANCE = "settings___change_octoprint_instnace"
const val MENU_ITEM_PRINT_NOTIFICATION = "settings___print_notification"
const val MENU_ITEM_SCREEN_ON_DURING_PRINT = "settings___keep_screen_on_during__print"
const val MENU_ITEM_CANCEL_PRINT = "printer___cancel_print"
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


class MenuItemLibrary {

    private val map = mapOf(
        MENU_ITEM_SUPPORT_OCTOAPP to SupportOctoAppMenuItem::class,
        MENU_ITEM_SETTINGS_MENU to ShowSettingsMenuItem::class,
        MENU_ITEM_PRINTER_MENU to ShowPrinterMenuItem::class,
        MENU_ITEM_NEWS to ShowNewsMenuItem::class,
        MENU_ITEM_SEND_FEEDBACK to SendFeedbackMenuItem::class,
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
        MENU_ITEM_SHOW_WEBCAM to ShowWebcamMenuItem::class
    )

    operator fun get(itemId: String): MenuItem? = when {
        map.containsKey(itemId) -> map[itemId]?.java?.constructors?.firstOrNull()?.newInstance() as? MenuItem
        itemId.startsWith(MENU_ITEM_SWITCH_INSTANCE) -> SwitchInstanceMenuItem.forItemId(itemId)
        itemId.startsWith(MENU_ITEM_APPLY_TEMPERATURE_PRESET) -> ApplyTemperaturePresetMenuItem.forItemId(itemId)
        itemId.startsWith(MENU_EXECUTE_SYSTEM_COMMAND) -> ExecuteSystemCommandMenuItem.forItemId(itemId)
        else -> null
    }
}