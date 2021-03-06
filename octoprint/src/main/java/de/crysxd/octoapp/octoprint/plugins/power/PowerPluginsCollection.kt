package de.crysxd.octoapp.octoprint.plugins.power

import de.crysxd.octoapp.octoprint.exceptions.OctoPrintApiException
import de.crysxd.octoapp.octoprint.models.settings.Settings
import de.crysxd.octoapp.octoprint.plugins.power.psucontrol.PsuControlApi
import de.crysxd.octoapp.octoprint.plugins.power.psucontrol.PsuControlPowerPlugin
import de.crysxd.octoapp.octoprint.plugins.power.tasmota.TasmotaApi
import de.crysxd.octoapp.octoprint.plugins.power.tasmota.TasmotaPowerPlugin
import de.crysxd.octoapp.octoprint.plugins.power.tplinkplug.TpLinkSmartPlugApi
import de.crysxd.octoapp.octoprint.plugins.power.tplinkplug.TpLinkSmartPlugPowerPlugin
import de.crysxd.octoapp.octoprint.plugins.power.tradfri.*
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
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

suspend fun runWithPowerPluginFixes(powerDevice: PowerDevice, block: suspend () -> Unit) {
    try {
        // PSU Control takes a long time doing nothing
        withTimeout(2500) {
            block()
        }
    } catch (e: TimeoutCancellationException) {
        // Ignore
    } catch (e: OctoPrintApiException) {
        // Tradfri gives 500 on every action...ignore
        if (e.responseCode != 500 || powerDevice !is TradfriPowerDevice) {
            throw e
        }
    }
}