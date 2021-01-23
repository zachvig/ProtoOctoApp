package de.crysxd.octoapp.octoprint.plugins.power.tradfri

import de.crysxd.octoapp.octoprint.exceptions.OctoPrintApiException
import de.crysxd.octoapp.octoprint.models.settings.Settings
import de.crysxd.octoapp.octoprint.plugins.power.PowerDevice
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
        api.sendCommandWithResponse(TradfriCommand.GetDeviceStatus(device)).state == true

    override fun getDevices(settings: Settings) =
        settings.plugins.values.mapNotNull {
            it as? Settings.TradfriSettings
        }.firstOrNull()?.devices?.map {
            it.copy(plugin = this)
        } ?: emptyList()
}

suspend fun runWithTradfriFix(powerDevice: PowerDevice, block: suspend () -> Unit) {
    try {
        block()
    } catch (e: OctoPrintApiException) {
        // Tradfri gives 500 on every action...ignore
        if (e.responseCode != 500 || powerDevice !is TradfriPowerDevice) {
            throw e
        }
    }
}