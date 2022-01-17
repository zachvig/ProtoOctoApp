package de.crysxd.octoapp.octoprint.plugins.power.wemoswitch

import de.crysxd.octoapp.octoprint.models.settings.Settings
import de.crysxd.octoapp.octoprint.plugins.power.PowerPlugin

class WemoSwitchPowerPlugin(
    private val api: WemoSwitchApi
) : PowerPlugin<WemoSwitchPowerDevice> {

    internal suspend fun turnOn(device: WemoSwitchPowerDevice) {
        api.sendCommand(WemoSwitchCommand.TurnDeviceOn(device))
    }

    internal suspend fun turnOff(device: WemoSwitchPowerDevice) {
        api.sendCommand(WemoSwitchCommand.TurnDeviceOff(device))
    }

    internal suspend fun isOn(device: WemoSwitchPowerDevice) =
        api.sendCommandWithResponse(WemoSwitchCommand.GetDeviceStatus(device))?.currentState == WemoSwitchResponse.State.ON

    override fun getDevices(settings: Settings) =
        settings.plugins.values.mapNotNull {
            it as? Settings.TpLinkSmartPlugSettings
        }.firstOrNull()?.devices?.map {
            it.copy(plugin = this)
        } ?: emptyList()
}