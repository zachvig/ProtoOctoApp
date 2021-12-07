package de.crysxd.baseui.menu.main

import android.content.Context
import de.crysxd.baseui.R
import de.crysxd.baseui.menu.Menu
import de.crysxd.baseui.menu.MenuHost
import de.crysxd.baseui.menu.MenuItemStyle
import de.crysxd.baseui.menu.ToggleMenuItem
import de.crysxd.octoapp.base.UriLibrary
import de.crysxd.octoapp.base.billing.BillingManager
import de.crysxd.octoapp.base.billing.BillingManager.FEATURE_AUTOMATIC_LIGHTS
import de.crysxd.octoapp.base.di.BaseInjector
import de.crysxd.octoapp.base.ext.toHtml
import de.crysxd.octoapp.base.usecase.GetPowerDevicesUseCase
import de.crysxd.octoapp.octoprint.plugins.power.PowerDevice
import kotlinx.parcelize.Parcelize

@Parcelize
class AutomaticLightsSettingsMenu : Menu {

    private val isFeatureEnabled get() = BillingManager.isFeatureEnabled(FEATURE_AUTOMATIC_LIGHTS)

    override suspend fun getMenuItem(): List<ToggleMenuItem> {
        if (!isFeatureEnabled) return emptyList()

        val lights = BaseInjector.get().getPowerDevicesUseCase().execute(
            GetPowerDevicesUseCase.Params(
                queryState = false,
                requiredCapabilities = listOf(PowerDevice.Capability.Illuminate)
            )
        ).filter {
            it.first.controlMethods.contains(PowerDevice.ControlMethod.TurnOnOff)
        }.map {
            LightSettingMenuItem(it.first)
        }

        return listOf(
            listOfNotNull(AutoLightsForWidgetMenuItem().takeIf { lights.isNotEmpty() }),
            lights
        ).flatten()
    }

    override suspend fun getTitle(context: Context) = context.getString(R.string.main_menu___title_automatic_lights)

    override suspend fun getSubtitle(context: Context) = context.getString(R.string.main_menu___subtitle_automatic_lights)

    override fun getBottomText(context: Context) = context.getString(R.string.main_menu___warning_automatic_lights).toHtml()

    override fun getEmptyStateIcon() = R.drawable.octo_power_devices

    override fun getEmptyStateActionText(context: Context) = context.getString(
        if (isFeatureEnabled) R.string.power_menu___empty_state_action else R.string.main_menu___button_enable_automatic_lights
    )

    override fun getEmptyStateActionUrl(context: Context) =
        (if (isFeatureEnabled) UriLibrary.getFaqUri("supported_plugin") else UriLibrary.getPurchaseUri()).toString()

    override fun getEmptyStateSubtitle(context: Context) = context.getString(
        if (isFeatureEnabled) R.string.main_menu___empty_state_subtitle_automatic_lights else R.string.main_menu___empty_state_subtitle_automatic_lights_disabled
    )

    class AutoLightsForWidgetMenuItem : ToggleMenuItem() {
        private val prefs get() = BaseInjector.get().octoPreferences()
        override val isEnabled get() = prefs.automaticLightsForWidgetRefresh
        override val itemId = "light_for_widget_refresh"
        override var groupId = "0"
        override val order = 200
        override val canBePinned = false
        override val style = MenuItemStyle.Settings
        override val icon = R.drawable.ic_round_widgets_24
        override fun getTitle(context: Context) = context.getString(R.string.main_menu___item_lights_for_widget)
        override fun getDescription(context: Context) = context.getString(R.string.main_menu___item_lights_for_widget_description)

        override suspend fun handleToggleFlipped(host: MenuHost, enabled: Boolean) {
            prefs.automaticLightsForWidgetRefresh = enabled
        }
    }

    class LightSettingMenuItem(private val device: PowerDevice) : ToggleMenuItem() {
        private val prefs get() = BaseInjector.get().octoPreferences()
        override val isEnabled get() = prefs.automaticLights.contains(device.id)
        override val itemId = "light_settings/${device.id}"
        override var groupId = "lights"
        override val order = 100
        override val canBePinned = false
        override val style = MenuItemStyle.Settings
        override val icon = R.drawable.ic_round_wb_incandescent_24
        override fun getTitle(context: Context) = device.displayName

        override suspend fun handleToggleFlipped(host: MenuHost, enabled: Boolean) {
            val base = prefs.automaticLights.toMutableSet()
            if (enabled) base.add(device.id) else base.remove(device.id)
            prefs.automaticLights = base
        }
    }
}