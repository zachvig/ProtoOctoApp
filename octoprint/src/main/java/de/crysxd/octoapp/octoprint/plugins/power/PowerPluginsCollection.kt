package de.crysxd.octoapp.octoprint.plugins.power

import de.crysxd.octoapp.octoprint.models.settings.Settings
import de.crysxd.octoapp.octoprint.plugins.power.gpiocontrol.GpioControlPowerPlugin
import de.crysxd.octoapp.octoprint.plugins.power.gpiocontrol.GpioCoontrolApi
import de.crysxd.octoapp.octoprint.plugins.power.mystrom.MyStromApi
import de.crysxd.octoapp.octoprint.plugins.power.mystrom.MyStromPowerPlugin
import de.crysxd.octoapp.octoprint.plugins.power.ocotrelay.OctoRelayApi
import de.crysxd.octoapp.octoprint.plugins.power.ocotrelay.OctoRelayPowerPlugin
import de.crysxd.octoapp.octoprint.plugins.power.octocam.OctoCamApi
import de.crysxd.octoapp.octoprint.plugins.power.octocam.OctoCamPowerPlugin
import de.crysxd.octoapp.octoprint.plugins.power.octolight.OctoLightApi
import de.crysxd.octoapp.octoprint.plugins.power.octolight.OctoLightPowerPlugin
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
import de.crysxd.octoapp.octoprint.plugins.power.wemoswitch.WemoSwitchApi
import de.crysxd.octoapp.octoprint.plugins.power.wemoswitch.WemoSwitchPowerPlugin
import de.crysxd.octoapp.octoprint.plugins.power.wled.WledApi
import de.crysxd.octoapp.octoprint.plugins.power.wled.WledPowerPlugin
import de.crysxd.octoapp.octoprint.plugins.power.ws281x.WS281xApi
import de.crysxd.octoapp.octoprint.plugins.power.ws281x.WS281xPowerPlugin
import retrofit2.Retrofit

class PowerPluginsCollection(retrofit: Retrofit) {

    val plugins = listOf(
        PsuControlPowerPlugin(retrofit.create(PsuControlApi::class.java)),
        TradfriPowerPlugin(retrofit.create(TradfriApi::class.java)),
        TpLinkSmartPlugPowerPlugin(retrofit.create(TpLinkSmartPlugApi::class.java)),
        TasmotaPowerPlugin(retrofit.create(TasmotaApi::class.java)),
        TuyaPowerPlugin(retrofit.create(TuyaApi::class.java)),
        WS281xPowerPlugin(retrofit.create(WS281xApi::class.java)),
        GpioControlPowerPlugin(retrofit.create(GpioCoontrolApi::class.java)),
        MyStromPowerPlugin(retrofit.create(MyStromApi::class.java)),
        OctoRelayPowerPlugin(retrofit.create(OctoRelayApi::class.java)),
        WledPowerPlugin(retrofit.create(WledApi::class.java)),
        OctoCamPowerPlugin(retrofit.create(OctoCamApi::class.java)),
        OctoLightPowerPlugin(retrofit.create(OctoLightApi::class.java)),
        WemoSwitchPowerPlugin(retrofit.create(WemoSwitchApi::class.java)),
    )

    fun getDevices(settings: Settings) = plugins.map {
        it.getDevices(settings)
    }.flatten()
}