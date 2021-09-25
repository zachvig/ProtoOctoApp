package de.crysxd.baseui.menu.power

import android.content.Context
import de.crysxd.baseui.R
import de.crysxd.baseui.menu.Menu
import de.crysxd.baseui.menu.MenuItem
import de.crysxd.octoapp.base.di.BaseInjector
import de.crysxd.octoapp.base.ext.toHtml
import de.crysxd.octoapp.base.usecase.GetPowerDevicesUseCase
import de.crysxd.octoapp.octoprint.plugins.power.PowerDevice
import kotlinx.parcelize.Parcelize

@Parcelize
class PowerDeviceMenu(
    private val uniqueDeviceId: String,
    private val name: String,
    private val pluginName: String,
) : Menu {

    override suspend fun getMenuItem(): List<MenuItem> {
        val device = BaseInjector.get().getPowerDevicesUseCase().execute(
            GetPowerDevicesUseCase.Params(
                queryState = false, onlyGetDeviceWithUniqueId = uniqueDeviceId
            )
        ).first().first

        return listOfNotNull(
            PowerControlsMenu.CyclePowerDeviceMenuItem(uniqueDeviceId = uniqueDeviceId, pluginName = pluginName, name = name, showName = false)
                .takeIf { device.controlMethods.contains(PowerDevice.ControlMethod.TurnOnOff) },
            PowerControlsMenu.TurnPowerDeviceOffMenuItem(uniqueDeviceId = uniqueDeviceId, pluginName = pluginName, name = name, showName = false)
                .takeIf { device.controlMethods.contains(PowerDevice.ControlMethod.TurnOnOff) },
            PowerControlsMenu.TurnPowerDeviceOnMenuItem(uniqueDeviceId = uniqueDeviceId, pluginName = pluginName, name = name, showName = false)
                .takeIf { device.controlMethods.contains(PowerDevice.ControlMethod.TurnOnOff) },
            PowerControlsMenu.TogglePowerDeviceMenuItem(uniqueDeviceId = uniqueDeviceId, pluginName = pluginName, name = name, showName = false)
                .takeIf { device.controlMethods.contains(PowerDevice.ControlMethod.Toggle) },
        )
    }

    override suspend fun getTitle(context: Context) = name
    override fun getBottomText(context: Context) = context.getString(R.string.power_menu___device_provided_by_x, pluginName).toHtml()
    override suspend fun getSubtitle(context: Context) =
        when (BaseInjector.get().getPowerDevicesUseCase().execute(GetPowerDevicesUseCase.Params(queryState = true, onlyGetDeviceWithUniqueId = uniqueDeviceId))
            .first().second) {
            GetPowerDevicesUseCase.PowerState.On -> context.getString(R.string.power_menu___device_is_on)
            GetPowerDevicesUseCase.PowerState.Off -> context.getString(R.string.power_menu___device_is_off)
            GetPowerDevicesUseCase.PowerState.Unknown -> null
        }
}