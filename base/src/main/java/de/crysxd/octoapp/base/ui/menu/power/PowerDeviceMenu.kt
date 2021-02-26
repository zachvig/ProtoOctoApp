package de.crysxd.octoapp.base.ui.menu.power

import android.content.Context
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.ext.toHtml
import de.crysxd.octoapp.base.ui.menu.Menu
import de.crysxd.octoapp.base.ui.menu.MenuItem
import de.crysxd.octoapp.base.usecase.GetPowerDevicesUseCase
import kotlinx.android.parcel.Parcelize

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
    override fun getBottomText(context: Context) = "<small>This device is provided by $pluginName</small>".toHtml()
    override suspend fun getSubtitle(context: Context) =
        when (Injector.get().getPowerDevicesUseCase().execute(GetPowerDevicesUseCase.Params(queryState = true, onlyGetDeviceWithUniqueId = uniqueDeviceId))
            .first().second) {
            GetPowerDevicesUseCase.PowerState.On -> "Device is on"
            GetPowerDevicesUseCase.PowerState.Off -> "Device is off"
            GetPowerDevicesUseCase.PowerState.Unknown -> null
        }
}