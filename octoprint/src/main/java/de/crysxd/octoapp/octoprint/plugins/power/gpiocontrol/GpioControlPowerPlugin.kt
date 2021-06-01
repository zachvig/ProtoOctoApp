package de.crysxd.octoapp.octoprint.plugins.power.gpiocontrol

import de.crysxd.octoapp.octoprint.models.settings.Settings
import de.crysxd.octoapp.octoprint.plugins.power.PowerPlugin
import de.crysxd.octoapp.octoprint.plugins.power.tasmota.TasmotaPowerDevice
import de.crysxd.octoapp.octoprint.plugins.power.tasmota.TasmotaResponse

class GpioControlPowerPlugin(
    private val api: GpioCoontrolApi
) : PowerPlugin<GpioControlPowerDevice> {

    internal suspend fun turnOn(device: GpioControlPowerDevice) {
        api.sendCommand(GpioControlCommand.TurnGpioOn(device))
    }

    internal suspend fun turnOff(device: GpioControlPowerDevice) {
        api.sendCommand(GpioControlCommand.TurnGpioOff(device))
    }

    internal suspend fun isOn(device: GpioControlPowerDevice) =
        api.getGpioState().getOrNull(device.index) == GpioState.ON

    override fun getDevices(settings: Settings) =
        settings.plugins.values.mapNotNull {
            it as? Settings.GpioControlSettings
        }.firstOrNull()?.devices?.map {
            it.copy(plugin = this)
        } ?: emptyList()
}