package de.crysxd.octoapp.octoprint.plugins.power.ws281x

import de.crysxd.octoapp.octoprint.models.settings.Settings
import de.crysxd.octoapp.octoprint.plugins.power.PowerPlugin

class WS281xPowerPlugin(
    private val api: WS281xApi
) : PowerPlugin<WS281xDevice> {
    internal suspend fun turnOn(device: WS281xDevice) = when (device) {
        is WS281xDevice.WS281xLightsDevice -> api.sendCommand(WS281xCommand.TurnLightsOn)
        is WS281xDevice.WS281xTorchDevice -> api.sendCommand(WS281xCommand.TurnTorchOn)
    }

    internal suspend fun turnOff(device: WS281xDevice) = when (device) {
        is WS281xDevice.WS281xLightsDevice -> api.sendCommand(WS281xCommand.TurnLightsOff)
        is WS281xDevice.WS281xTorchDevice -> api.sendCommand(WS281xCommand.TurnTorchOff)
    }

    internal suspend fun isOn(device: WS281xDevice) = api.getStatus().let {
        when (device) {
            is WS281xDevice.WS281xLightsDevice -> it.lightsOn
            is WS281xDevice.WS281xTorchDevice -> it.torchOn
        }
    }

    override fun getDevices(settings: Settings) =
        settings.plugins.values.mapNotNull {
            it as? Settings.WS281xSettings
        }.firstOrNull()?.let {
            listOf(
                WS281xDevice.WS281xTorchDevice(this),
                WS281xDevice.WS281xLightsDevice(this),
            )
        } ?: emptyList()
}