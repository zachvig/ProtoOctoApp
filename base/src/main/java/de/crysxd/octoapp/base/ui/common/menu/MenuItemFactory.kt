package de.crysxd.octoapp.base.ui.common.menu

import kotlin.reflect.KClass

const val MENU_ITEM_SUPPORT_OCTOAPP = "main___support_octoapp"
const val MENU_ITEM_SETTINGS_MENU = "main___settings_menu"
const val MENU_ITEM_PRINTER_MENU = "main___printer_menu"
const val MENU_ITEM_NEWS = "main___news"
const val MENU_ITEM_SEND_FEEDBACK = "settings___send_feedback"
const val MENU_ITEM_CHANGE_LANGUAGE = "main___change_language"
const val MENU_ITEM_OPEN_OCTOPRINT = "main___open_octoprint"
const val MENU_ITEM_CHANGE_OCTOPRINT_INSTANCE = "main___change_octoprint_instnace"


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
    )

    operator fun get(itemId: String): KClass<out MenuItem>? = map[itemId]
}