package de.crysxd.baseui.menu.main

import de.crysxd.baseui.menu.MenuItem
import de.crysxd.baseui.menu.material.MaterialPluginMenu
import de.crysxd.baseui.menu.power.PowerControlsMenu
import de.crysxd.baseui.menu.switchprinter.AddInstanceMenuItem
import de.crysxd.baseui.menu.switchprinter.SwitchInstanceMenuItem
import de.crysxd.baseui.menu.temperature.ApplyTemperaturePresetForAllMenuItem
import de.crysxd.baseui.menu.temperature.ApplyTemperaturePresetForBedMenuItem
import de.crysxd.baseui.menu.temperature.ApplyTemperaturePresetForChamberMenuItem
import de.crysxd.baseui.menu.temperature.ApplyTemperaturePresetForHotendMenuItem
import de.crysxd.baseui.menu.temperature.ApplyTemperaturePresetMenuItem
import de.crysxd.baseui.menu.webcam.WebcamSettingsMenu
import de.crysxd.octoapp.base.data.models.MenuItems
import de.crysxd.octoapp.base.di.BaseInjector
import timber.log.Timber


class MenuItemLibrary {

    private val map = mapOf(
        MenuItems.MENU_ITEM_SETTINGS_MENU to ShowSettingsMenuItem::class,
        MenuItems.MENU_ITEM_PRINTER_MENU to ShowPrinterMenuItem::class,
        MenuItems.MENU_ITEM_TUTORIALS to ShowTutorialsMenuItem::class,
        MenuItems.MENU_ITEM_CHANGE_LANGUAGE to ChangeLanguageMenuItem::class,
        MenuItems.MENU_ITEM_OPEN_OCTOPRINT to OpenOctoPrintMenuItem::class,
        MenuItems.MENU_ITEM_SHOW_CHANGE_OCTOPRINT_MENU to ChangeOctoPrintInstanceMenuItem::class,
        MenuItems.MENU_ITEM_CANCEL_PRINT to CancelPrintMenuItem::class,
        MenuItems.MENU_ITEM_EMERGENCY_STOP to EmergencyStopMenuItem::class,
        MenuItems.MENU_ITEM_TURN_PSU_OFF to TurnPsuOffMenuItem::class,
        MenuItems.MENU_ITEM_POWER_CONTROLS to OpenPowerControlsMenuItem::class,
        MenuItems.MENU_ITEM_NIGHT_THEME to AppThemeMenuItem::class,
        MenuItems.MENU_ITEM_PRINT_NOTIFICATION_SETTINGS to PrintNotificationMenuItem::class,
        MenuItems.MENU_ITEM_LIVE_NOTIFICATION to PrintNotificationsMenu.LiveNotificationMenuItem::class,
        MenuItems.MENU_ITEM_SYSTEM_NOTIFICATION_SETTINGS to PrintNotificationsMenu.SystemNotificationSettings::class,
        MenuItems.MENU_ITEM_SCREEN_ON_DURING_PRINT to KeepScreenOnDuringPrintMenuItem::class,
        MenuItems.MENU_ITEM_ADD_INSTANCE to AddInstanceMenuItem::class,
        MenuItems.MENU_ITEM_SHOW_WEBCAM to ShowWebcamMenuItem::class,
        MenuItems.MENU_ITEM_CANCEL_PRINT_KEEP_TEMPS to CancelPrintKeepTemperaturesMenuItem::class,
        MenuItems.MENU_ITEM_TEMPERATURE_MENU to ShowTemperatureMenuItem::class,
        MenuItems.MENU_ITEM_AUTO_CONNECT_PRINTER to AutoConnectPrinterMenuItem::class,
        MenuItems.MENU_ITEM_MATERIAL_MENU to ShowMaterialPluginMenuItem::class,
        MenuItems.MENU_ITEM_HELP to HelpMenuItem::class,
        MenuItems.MENU_ITEM_CUSTOMIZE_WIDGETS to CustomizeWidgetsMenuItem::class,
        MenuItems.MENU_ITEM_OPEN_TERMINAL to OpenTerminalMenuItem::class,
        MenuItems.MENU_ITEM_SHOW_OCTOAPP_LAB to ShowOctoAppLabMenuItem::class,
        MenuItems.MENU_ITEM_ROTATE_APP to OctoAppLabMenu.RotationMenuItem::class,
        MenuItems.MENU_ITEM_CONFIGURE_REMOTE_ACCESS to ConfigureRemoteAccessMenuItem::class,
        MenuItems.MENU_ITEM_SHOW_FILES to ShowFilesMenuItem::class,
        MenuItems.MENU_ITEM_AUTOMATIC_LIGHTS to AutomaticLightsSettingsMenuItem::class,
        MenuItems.MENU_ITEM_SHOW_WEBCAM_RESOLUTION to WebcamSettingsMenu.ShowResolutionMenuItem::class,
        MenuItems.MENU_ITEM_ENABLE_FULL_WEBCAM_RESOLUTION to WebcamSettingsMenu.EnableFullResolutionMenuItem::class,
        MenuItems.MENU_ITEM_CONFIRM_POWER_OFF to ConfirmPowerOffSettingsMenuItem::class,
        MenuItems.MENU_ITEM_PLUGINS to ShowPluginLibraryOctoPrintMenuItem::class,
        MenuItems.MENU_ITEM_COOL_DOWN to CoolDownMenuItem::class,
    )

    operator fun get(itemId: String): MenuItem? = when {
        map.containsKey(itemId) -> try {
            map[itemId]?.java?.constructors?.firstOrNull()?.newInstance() as? MenuItem
        } catch (e: Exception) {
            Timber.e(e, "Unable to inflate menu item with itemId=$itemId")
            null
        }
        itemId.startsWith(MenuItems.MENU_ITEM_SWITCH_INSTANCE) -> SwitchInstanceMenuItem.forItemId(itemId)
        itemId.startsWith(MenuItems.MENU_ITEM_APPLY_TEMPERATURE_PRESET_ALL) -> ApplyTemperaturePresetForAllMenuItem.forItemId(itemId)
        itemId.startsWith(MenuItems.MENU_ITEM_APPLY_TEMPERATURE_PRESET_HOTEND) -> ApplyTemperaturePresetForHotendMenuItem.forItemId(itemId)
        itemId.startsWith(MenuItems.MENU_ITEM_APPLY_TEMPERATURE_PRESET_BED) -> ApplyTemperaturePresetForBedMenuItem.forItemId(itemId)
        itemId.startsWith(MenuItems.MENU_ITEM_APPLY_TEMPERATURE_PRESET_CHAMBER) -> ApplyTemperaturePresetForChamberMenuItem.forItemId(itemId)
        itemId.startsWith(MenuItems.MENU_ITEM_APPLY_TEMPERATURE_PRESET) -> ApplyTemperaturePresetMenuItem.forItemId(itemId)
        itemId.startsWith(MenuItems.MENU_EXECUTE_SYSTEM_COMMAND) -> ExecuteSystemCommandMenuItem.forItemId(itemId)
        itemId.startsWith(MenuItems.MENU_ITEM_ACTIVATE_MATERIAL) -> MaterialPluginMenu.ActivateMaterialMenuItem.forItemId(itemId)
        itemId.startsWith(MenuItems.MENU_ITEM_SHOW_POWER_DEVICE_ACTIONS) -> PowerControlsMenu.ShowPowerDeviceActionsMenuItem.forItemId(itemId)
        itemId.startsWith(MenuItems.MENU_ITEM_POWER_DEVICE_OFF) -> PowerControlsMenu.TurnPowerDeviceOffMenuItem.forItemId(itemId)
        itemId.startsWith(MenuItems.MENU_ITEM_POWER_DEVICE_ON) -> PowerControlsMenu.TurnPowerDeviceOnMenuItem.forItemId(itemId)
        itemId.startsWith(MenuItems.MENU_ITEM_POWER_DEVICE_CYCLE) -> PowerControlsMenu.CyclePowerDeviceMenuItem.forItemId(itemId)
        itemId.startsWith(MenuItems.MENU_ITEM_POWER_DEVICE_TOGGLE) -> PowerControlsMenu.TogglePowerDeviceMenuItem.forItemId(itemId)
        itemId.startsWith(MenuItems.MENU_ITEM_WEBCAM_ASPECT_RATIO_SOURCE) -> WebcamSettingsMenu.AspectRatioMenuItem(BaseInjector.get().localizedContext())
        else -> null
    }
}