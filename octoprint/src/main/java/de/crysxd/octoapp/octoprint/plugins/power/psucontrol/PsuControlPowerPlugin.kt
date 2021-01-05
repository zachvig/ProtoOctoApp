package de.crysxd.octoapp.octoprint.plugins.power.psucontrol

import de.crysxd.octoapp.octoprint.models.settings.Settings
import de.crysxd.octoapp.octoprint.plugins.power.PowerPlugin

class PsuControlPowerPlugin(
    private val psuApi: PsuControlApi
) : PowerPlugin<PsuControlPowerDevice> {

    internal suspend fun turnOn() {
        psuApi.sendPsuCommand(PsuCommand.TurnOnPsuCommand)
    }

    internal suspend fun turnOff() {
        psuApi.sendPsuCommand(PsuCommand.TurnOffPsuCommand)
    }

    internal suspend fun isOn() =
        psuApi.sendPsuCommandWithResponse(PsuCommand.GetPsuStateCommand).isPSUOn == true

    override fun getDevices(settings: Settings) = if (settings.plugins.containsKey("psucontrol")) {
        listOf(PsuControlPowerDevice(this))
    } else {
        emptyList()
    }
}