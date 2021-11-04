package de.crysxd.octoapp.octoprint.plugins.power.octolight

import de.crysxd.octoapp.octoprint.models.settings.Settings
import de.crysxd.octoapp.octoprint.plugins.power.PowerPlugin

class OctoLightPowerPlugin(
    private val octoLightApi: OctoLightApi
) : PowerPlugin<OctoLightPowerDevice> {

    internal suspend fun toggle() = octoLightApi.toggleLight()

    override fun getDevices(settings: Settings) = settings.plugins.filterValues {
        it is Settings.OctoLightSettings
    }.map {
        OctoLightPowerDevice(this)
    }
}