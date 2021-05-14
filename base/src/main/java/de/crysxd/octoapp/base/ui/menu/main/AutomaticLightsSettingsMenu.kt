package de.crysxd.octoapp.base.ui.menu.main

import android.content.Context
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.UriLibrary
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.ui.menu.Menu
import de.crysxd.octoapp.base.ui.menu.MenuBottomSheetFragment
import de.crysxd.octoapp.base.ui.menu.MenuItemStyle
import de.crysxd.octoapp.base.ui.menu.ToggleMenuItem
import de.crysxd.octoapp.base.usecase.GetPowerDevicesUseCase
import de.crysxd.octoapp.octoprint.plugins.power.PowerDevice
import kotlinx.parcelize.Parcelize

@Parcelize
class AutomaticLightsSettingsMenu : Menu {

    override suspend fun getMenuItem(): List<ToggleMenuItem> {
        val lights = Injector.get().getPowerDevicesUseCase().execute(
            GetPowerDevicesUseCase.Params(
                queryState = false,
                requiredCapabilities = listOf(PowerDevice.Capability.Illuminate)
            )
        ).map {
            LightSettingMenuItem(it.first)
        }

        return listOf(
            listOfNotNull(AutoLightsForWidgetMenuItem().takeIf { lights.isNotEmpty() }),
            lights
        ).flatten()
    }

    override suspend fun getTitle(context: Context) = context.getString(R.string.main_menu___title_automatic_lights)

    override suspend fun getSubtitle(context: Context) = context.getString(R.string.main_menu___subtitle_automatic_lights)

    override fun getEmptyStateIcon() = R.drawable.octo_power_devices
    override fun getEmptyStateActionText(context: Context) = context.getString(R.string.power_menu___empty_state_action)
    override fun getEmptyStateActionUrl(context: Context) = UriLibrary.getFaqUri("supported_plugin").toString()
    override fun getEmptyStateSubtitle(context: Context) = context.getString(R.string.main_menu___empty_state_subtitle_automatic_lights)

    class AutoLightsForWidgetMenuItem : ToggleMenuItem() {
        private val prefs get() = Injector.get().octoPreferences()
        override val isEnabled get() = prefs.automaticLightsForWidgetRefresh
        override val itemId = "light_for_widget_refresh"
        override var groupId = "0"
        override val order = 200
        override val canBePinned = false
        override val style = MenuItemStyle.Settings
        override val icon = R.drawable.ic_round_widgets_24
        override suspend fun getTitle(context: Context) = context.getString(R.string.main_menu___item_lights_for_widget)
        override suspend fun getDescription(context: Context) = context.getString(R.string.main_menu___item_lights_for_widget_description)

        override suspend fun handleToggleFlipped(host: MenuBottomSheetFragment, enabled: Boolean) {
            prefs.automaticLightsForWidgetRefresh = enabled
        }
    }

    class LightSettingMenuItem(private val device: PowerDevice) : ToggleMenuItem() {
        private val prefs get() = Injector.get().octoPreferences()
        override val isEnabled get() = prefs.automaticLights.contains(device.id)
        override val itemId = "light_settings/${device.id}"
        override var groupId = "lights"
        override val order = 100
        override val canBePinned = false
        override val style = MenuItemStyle.Settings
        override val icon = R.drawable.ic_round_wb_incandescent_24
        override suspend fun getTitle(context: Context) = device.displayName

        override suspend fun handleToggleFlipped(host: MenuBottomSheetFragment, enabled: Boolean) {
            val base = prefs.automaticLights.toMutableSet()
            if (enabled) base.add(device.id) else base.remove(device.id)
            prefs.automaticLights = base
        }
    }
}