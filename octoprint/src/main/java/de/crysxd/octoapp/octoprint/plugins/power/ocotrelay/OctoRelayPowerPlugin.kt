package de.crysxd.octoapp.octoprint.plugins.power.ocotrelay

import de.crysxd.octoapp.octoprint.models.settings.Settings
import de.crysxd.octoapp.octoprint.plugins.power.PowerPlugin

class OctoRelayPowerPlugin(
    private val octoRelayApi: OctoRelayApi
) : PowerPlugin<OctoRelayPowerDevice> {

    internal suspend fun toggle(pin: String) {
        octoRelayApi.sendCommand(OctoRelayCommand.Toggle(pin))
    }

    override fun getDevices(settings: Settings) = settings.plugins.values.mapNotNull {
        it as? Settings.OctoRelaySettings
    }.map {
        it.devices?.map { it.copy(plugin = this) } ?: emptyList()
    }.flatten()
}