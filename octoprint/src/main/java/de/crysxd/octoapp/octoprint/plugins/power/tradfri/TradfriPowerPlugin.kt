package de.crysxd.octoapp.octoprint.plugins.power.tradfri

import de.crysxd.octoapp.octoprint.exceptions.OctoPrintApiException
import de.crysxd.octoapp.octoprint.models.settings.Settings
import de.crysxd.octoapp.octoprint.plugins.power.PowerPlugin

class TradfriPowerPlugin(
    private val api: TradfriApi
) : PowerPlugin<TradfriPowerDevice> {

    internal suspend fun turnOn(device: TradfriPowerDevice) {
        runWithFixes {
            api.sendCommand(TradfriCommand.TurnDeviceOn(device))
        } ?: Unit
    }

    internal suspend fun turnOff(device: TradfriPowerDevice) {
        runWithFixes {
            api.sendCommand(TradfriCommand.TurnDeviceOff(device))
        }
    }

    internal suspend fun isOn(device: TradfriPowerDevice) = runWithFixes {
        api.sendCommandWithResponse(TradfriCommand.GetDeviceStatus(device)).state
    } == true

    private suspend fun <T> runWithFixes(block: suspend () -> T): T? = try {
        block()
    } catch (e: OctoPrintApiException) {
        // Tradfri gives 500 on every action...ignore
        if (e.responseCode != 500) {
            throw e
        }

        null
    }

    override fun getDevices(settings: Settings) =
        settings.plugins.values.mapNotNull {
            it as? Settings.TradfriSettings
        }.firstOrNull()?.devices?.map {
            it.copy(plugin = this)
        } ?: emptyList()
}