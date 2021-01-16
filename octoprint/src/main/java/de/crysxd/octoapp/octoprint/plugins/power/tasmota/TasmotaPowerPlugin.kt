package de.crysxd.octoapp.octoprint.plugins.power.tasmota

import de.crysxd.octoapp.octoprint.models.settings.Settings
import de.crysxd.octoapp.octoprint.plugins.power.PowerPlugin

class TasmotaPowerPlugin(
    private val api: TasmotaApi
) : PowerPlugin<TasmotaPowerDevice> {

    internal suspend fun turnOn(device: TasmotaPowerDevice) {
        api.sendCommand(TasmotaCommand.TurnDeviceOn(device))
    }

    internal suspend fun turnOff(device: TasmotaPowerDevice) {
        api.sendCommand(TasmotaCommand.TurnDeviceOff(device))
    }

    internal suspend fun isOn(device: TasmotaPowerDevice) =
        api.sendCommandWithResponse(TasmotaCommand.GetDeviceStatus(device)).currentState == TasmotaResponse.State.ON

    override fun getDevices(settings: Settings) =
        settings.plugins.values.mapNotNull {
            it as? Settings.TasmotaSettings
        }.firstOrNull()?.devices?.map {
            it.copy(plugin = this)
        } ?: emptyList()
}