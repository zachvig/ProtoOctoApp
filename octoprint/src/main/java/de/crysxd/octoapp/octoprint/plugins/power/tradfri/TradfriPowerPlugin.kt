package de.crysxd.octoapp.octoprint.plugins.power.tradfri

import de.crysxd.octoapp.octoprint.models.settings.Settings
import de.crysxd.octoapp.octoprint.plugins.power.PowerPlugin

class TradfriPowerPlugin(
    private val api: TradfriApi
) : PowerPlugin<TradfriPowerDevice> {

    internal suspend fun turnOn(device: TradfriPowerDevice) {
        api.sendCommand(TradfriCommand.TurnDeviceOn(device))
    }

    internal suspend fun turnOff(device: TradfriPowerDevice) {
        api.sendCommand(TradfriCommand.TurnDeviceOff(device))
    }

    internal suspend fun isOn(device: TradfriPowerDevice) =
        api.sendCommand(TradfriCommand.GetDeviceStatus(device))?.state == true

    override fun getDevices(settings: Settings) =
        settings.plugins.values.mapNotNull {
            it as? Settings.TradfriSettings
        }.firstOrNull()?.devices?.map {
            it.copy(plugin = this)
        } ?: emptyList()
}