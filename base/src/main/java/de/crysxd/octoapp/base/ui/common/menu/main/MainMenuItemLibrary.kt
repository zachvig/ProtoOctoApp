package de.crysxd.octoapp.base.ui.common.menu.main

import de.crysxd.octoapp.base.ui.common.menu.MenuItem
import kotlin.reflect.KClass

const val MENU_ITEM_SUPPORT_OCTOAPP = "main___support_octoapp"
const val MENU_ITEM_SETTINGS_MENU = "main___settings_menu"
const val MENU_ITEM_PRINTER_MENU = "main___printer_menu"
const val MENU_ITEM_NEWS = "main___news"
const val MENU_ITEM_SEND_FEEDBACK = "settings___send_feedback"
const val MENU_ITEM_CHANGE_LANGUAGE = "settings___change_language"
const val MENU_ITEM_OPEN_OCTOPRINT = "settings___open_octoprint"
const val MENU_ITEM_NIGHT_THEME = "settings___night_theme"
const val MENU_ITEM_CHANGE_OCTOPRINT_INSTANCE = "settings___change_octoprint_instnace"
const val MENU_ITEM_PRINT_NOTIFICATION = "settings___print_notification"
const val MENU_ITEM_CANCEL_PRINT = "printer___cancel_print"
const val MENU_ITEM_EMERGENCY_STOP = "printer___cemergency_stop"
const val MENU_ITEM_TURN_PSU_OFF = "printer___turn_psu_off"
const val MENU_ITEM_POWER_CONTROLS = "printer___open_power_controls"


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
        MENU_ITEM_PRINT_NOTIFICATION to PrintNotificationMenutItem::class,
    )

    operator fun get(itemId: String): KClass<out MenuItem>? = map[itemId]
}