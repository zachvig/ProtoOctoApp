package de.crysxd.octoapp.octoprint.plugins.power.tplinkplug

import de.crysxd.octoapp.octoprint.models.settings.Settings
import de.crysxd.octoapp.octoprint.plugins.power.PowerPlugin

class TpLinkSmartPlugPowerPlugin(
    private val api: TpLinkSmartPlugApi
) : PowerPlugin<TpLinkSmartPlugPowerDevice> {

    internal suspend fun turnOn(device: TpLinkSmartPlugPowerDevice) {
        api.sendCommand(TpLinkSmartPlugCommand.TurnDeviceOn(device))
    }

    internal suspend fun turnOff(device: TpLinkSmartPlugPowerDevice) {
        api.sendCommand(TpLinkSmartPlugCommand.TurnDeviceOff(device))
    }

    internal suspend fun isOn(device: TpLinkSmartPlugPowerDevice) =
        api.sendCommandWithResponse(TpLinkSmartPlugCommand.GetDeviceStatus(device)).currentState == TpLinkSmartPlugResponse.State.ON

    override fun getDevices(settings: Settings) =
        settings.plugins.values.mapNotNull {
            it as? Settings.TpLinkSmartPlugSettings
        }.firstOrNull()?.devices?.map {
            it.copy(plugin = this)
        } ?: emptyList()
}