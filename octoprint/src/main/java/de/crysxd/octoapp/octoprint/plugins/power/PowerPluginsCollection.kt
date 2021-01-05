package de.crysxd.octoapp.octoprint.plugins.power

import de.crysxd.octoapp.octoprint.models.settings.Settings
import de.crysxd.octoapp.octoprint.plugins.power.psucontrol.PsuControlApi
import de.crysxd.octoapp.octoprint.plugins.power.psucontrol.PsuControlPowerPlugin
import de.crysxd.octoapp.octoprint.plugins.power.tradfri.TradfriApi
import de.crysxd.octoapp.octoprint.plugins.power.tradfri.TradfriPowerPlugin
import retrofit2.Retrofit

class PowerPluginsCollection(retrofit: Retrofit) {

    val plugins = listOf(
        PsuControlPowerPlugin(retrofit.create(PsuControlApi::class.java)),
        TradfriPowerPlugin(retrofit.create(TradfriApi::class.java)),
    )

    fun getDevices(settings: Settings) = plugins.map {
        it.getDevices(settings)
    }.flatten()
}
