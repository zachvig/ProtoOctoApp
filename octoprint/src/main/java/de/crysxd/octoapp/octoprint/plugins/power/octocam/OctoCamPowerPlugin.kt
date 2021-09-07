package de.crysxd.octoapp.octoprint.plugins.power.octocam

import de.crysxd.octoapp.octoprint.models.settings.Settings
import de.crysxd.octoapp.octoprint.plugins.power.PowerPlugin

class OctoCamPowerPlugin(
    private val octoCamApi: OctoCamApi
) : PowerPlugin<OctoCamPowerDevice> {

    internal suspend fun turnOn() {
        octoCamApi.sendCommand(OctoCamCommand.TurnLightOn)
    }

    internal suspend fun turnOff() {
        octoCamApi.sendCommand(OctoCamCommand.TurnLightOff)
    }

    internal suspend fun toggle() {
        octoCamApi.sendCommand(OctoCamCommand.ToggleLight)
    }

    internal suspend fun isOn() =
        octoCamApi.sendCommand(null).torchOn == true

    override fun getDevices(settings: Settings) = settings.plugins.filterValues { it is Settings.OctoCamSettings }.map {
        OctoCamPowerDevice(this)
    }
}