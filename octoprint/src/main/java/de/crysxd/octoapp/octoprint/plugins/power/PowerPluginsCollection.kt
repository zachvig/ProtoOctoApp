package de.crysxd.octoapp.octoprint.plugins.power

import de.crysxd.octoapp.octoprint.models.settings.Settings
import de.crysxd.octoapp.octoprint.plugins.power.psucontrol.PsuControlApi
import de.crysxd.octoapp.octoprint.plugins.power.psucontrol.PsuControlPowerPlugin
import de.crysxd.octoapp.octoprint.plugins.power.tasmota.TasmotaApi
import de.crysxd.octoapp.octoprint.plugins.power.tasmota.TasmotaPowerPlugin
import de.crysxd.octoapp.octoprint.plugins.power.tplinkplug.TpLinkSmartPlugApi
import de.crysxd.octoapp.octoprint.plugins.power.tplinkplug.TpLinkSmartPlugPowerPlugin
import de.crysxd.octoapp.octoprint.plugins.power.tradfri.TradfriApi
import de.crysxd.octoapp.octoprint.plugins.power.tradfri.TradfriPowerPlugin
import de.crysxd.octoapp.octoprint.plugins.power.tradfri.TuyaApi
import de.crysxd.octoapp.octoprint.plugins.power.tradfri.TuyaPowerPlugin
import retrofit2.Retrofit

class PowerPluginsCollection(retrofit: Retrofit) {

    val plugins = listOf(
        PsuControlPowerPlugin(retrofit.create(PsuControlApi::class.java)),
        TradfriPowerPlugin(retrofit.create(TradfriApi::class.java)),
        TpLinkSmartPlugPowerPlugin(retrofit.create(TpLinkSmartPlugApi::class.java)),
        TasmotaPowerPlugin(retrofit.create(TasmotaApi::class.java)),
        TuyaPowerPlugin(retrofit.create(TuyaApi::class.java)),
    )

    fun getDevices(settings: Settings) = plugins.map {
        it.getDevices(settings)
    }.flatten()
}
