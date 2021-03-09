package de.crysxd.octoapp.base.ui.menu.power

import android.content.Context
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.ext.toHtml
import de.crysxd.octoapp.base.ui.menu.Menu
import de.crysxd.octoapp.base.ui.menu.MenuItem
import de.crysxd.octoapp.base.usecase.GetPowerDevicesUseCase
import kotlinx.parcelize.Parcelize

@Parcelize
class PowerDeviceMenu(
    private val uniqueDeviceId: String,
    private val name: String,
    private val pluginName: String,
) : Menu {

    override suspend fun getMenuItem(): List<MenuItem> {
        return listOf(
            PowerControlsMenu.CyclePowerDeviceMenuItem(uniqueDeviceId = uniqueDeviceId, pluginName = pluginName, name = name, showName = false),
            PowerControlsMenu.TurnPowerDeviceOffMenuItem(uniqueDeviceId = uniqueDeviceId, pluginName = pluginName, name = name, showName = false),
            PowerControlsMenu.TurnPowerDeviceOnMenuItem(uniqueDeviceId = uniqueDeviceId, pluginName = pluginName, name = name, showName = false)
        )
    }

    override suspend fun getTitle(context: Context) = name
    override fun getBottomText(context: Context) = context.getString(R.string.power_menu___device_provided_by_x, pluginName).toHtml()
    override suspend fun getSubtitle(context: Context) =
        when (Injector.get().getPowerDevicesUseCase().execute(GetPowerDevicesUseCase.Params(queryState = true, onlyGetDeviceWithUniqueId = uniqueDeviceId))
            .first().second) {
            GetPowerDevicesUseCase.PowerState.On -> context.getString(R.string.power_menu___device_is_on)
            GetPowerDevicesUseCase.PowerState.Off -> context.getString(R.string.power_menu___device_is_off)
            GetPowerDevicesUseCase.PowerState.Unknown -> null
        }
}