package de.crysxd.octoapp.octoprint.json

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import de.crysxd.octoapp.octoprint.models.settings.Settings
import java.lang.reflect.Type

class PluginSettingsDeserializer : JsonDeserializer<Settings.PluginSettingsGroup> {

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): Settings.PluginSettingsGroup {
        val obj = json.asJsonObject
        val settings = obj.keySet().toList().map {
            Pair(it, deserialize(context, it, obj.get(it)))
        }.toMap()

        val pluginSettings = Settings.PluginSettingsGroup()
        pluginSettings.putAll(settings)

        return pluginSettings
    }

    private fun deserialize(context: JsonDeserializationContext, key: String, element: JsonElement) = when (key) {
        "gcodeviewer" -> context.deserialize<Settings.GcodeViewerSettings>(element, Settings.GcodeViewerSettings::class.java)
        "ikea_tradfri" -> context.deserialize<Settings.TradfriSettings>(element, Settings.TradfriSettings::class.java)
        "tplinksmartplug" -> context.deserialize<Settings.TpLinkSmartPlugSettings>(element, Settings.TpLinkSmartPlugSettings::class.java)
        "tasmota" -> context.deserialize<Settings.TasmotaSettings>(element, Settings.TasmotaSettings::class.java)
        "gpiocontrol" -> deserializeGpioControlSettings(context, element)
        "tuyasmartplug" -> context.deserialize<Settings.TuyaSettings>(element, Settings.TuyaSettings::class.java)
        "multicam" -> context.deserialize<Settings.MultiCamSettings>(element, Settings.MultiCamSettings::class.java)
        "ws281x_led_status" -> context.deserialize<Settings.WS281xSettings>(element, Settings.WS281xSettings::class.java)
        "discovery" -> context.deserialize<Settings.Discovery>(element, Settings.Discovery::class.java)
        "octoeverywhere" -> context.deserialize<Settings.OctoEverywhere>(element, Settings.OctoEverywhere::class.java)
        else -> Unknown
    }

    private fun deserializeGpioControlSettings(context: JsonDeserializationContext, element: JsonElement): Settings.GpioControlSettings {
        // ID of devices is null! The ID is the index in the array...
        val raw = context.deserialize<Settings.GpioControlSettings>(element, Settings.GpioControlSettings::class.java)
        return raw.copy(devices = raw.devices.mapIndexed { index, device -> device.copy(index = index) })
    }

    private object Unknown : Settings.PluginSettings
}