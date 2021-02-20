package de.crysxd.octoapp.octoprint.plugins.power.tradfri

import de.crysxd.octoapp.octoprint.models.settings.Settings
import de.crysxd.octoapp.octoprint.plugins.power.PowerPlugin

class TuyaPowerPlugin(
    private val api: TuyaApi
) : PowerPlugin<TuyaPowerDevice> {

    internal suspend fun turnOn(device: TuyaPowerDevice) {
        api.sendCommand(TuyaCommand.TurnDeviceOn(device))
    }

    internal suspend fun turnOff(device: TuyaPowerDevice) {
        api.sendCommand(TuyaCommand.TurnDeviceOff(device))
    }

    internal suspend fun isOn(device: TuyaPowerDevice) =
        api.sendCommandWithResponse(TuyaCommand.GetDeviceStatus(device)).currentState == TuyaResponse.State.ON

    override fun getDevices(settings: Settings) =
        settings.plugins.values.mapNotNull {
            it as? Settings.TuyaSettings
        }.firstOrNull()?.devices?.map {
            it.copy(plugin = this)
        } ?: emptyList()
}