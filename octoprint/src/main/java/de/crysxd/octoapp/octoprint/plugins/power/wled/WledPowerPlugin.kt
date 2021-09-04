package de.crysxd.octoapp.octoprint.plugins.power.wled

import de.crysxd.octoapp.octoprint.models.settings.Settings
import de.crysxd.octoapp.octoprint.plugins.power.PowerPlugin

class WledPowerPlugin(
    private val api: WledApi
) : PowerPlugin<WledPowerDevice> {

    internal suspend fun turnOn() {
        api.sendCommand(WledCommand.TurnDeviceOn)
    }

    internal suspend fun turnOff() {
        api.sendCommand(WledCommand.TurnDeviceOff)
    }

    internal suspend fun isOn() = api.getStatus().lightsOn == true

    override fun getDevices(settings: Settings) = settings.plugins.values.filterIsInstance<Settings.WledSettings>().map {
        WledPowerDevice(plugin = this)
    }
}