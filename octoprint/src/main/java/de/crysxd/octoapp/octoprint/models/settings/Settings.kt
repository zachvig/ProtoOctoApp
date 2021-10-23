package de.crysxd.octoapp.octoprint.models.settings

import com.google.gson.annotations.SerializedName
import de.crysxd.octoapp.octoprint.plugins.power.gpiocontrol.GpioControlPowerDevice
import de.crysxd.octoapp.octoprint.plugins.power.ocotrelay.OctoRelayPowerDevice
import de.crysxd.octoapp.octoprint.plugins.power.tasmota.TasmotaPowerDevice
import de.crysxd.octoapp.octoprint.plugins.power.tplinkplug.TpLinkSmartPlugPowerDevice
import de.crysxd.octoapp.octoprint.plugins.power.tradfri.TradfriPowerDevice
import de.crysxd.octoapp.octoprint.plugins.power.tradfri.TuyaPowerDevice


data class Settings(
    val webcam: WebcamSettings,
    val plugins: PluginSettingsGroup,
    val temperature: TemperatureSettings,
    val terminalFilters: List<TerminalFilter>,
    val appearance: Appearance?,
) {

    data class Appearance(
        val name: String?,
        val color: String?
    )

    data class TerminalFilter(
        val name: String,
        val regex: String
    )

    data class TemperatureSettings(
        val profiles: List<TemperatureProfile>
    )

    data class TemperatureProfile(
        val bed: Int?,
        val chamber: Int?,
        val extruder: Int?,
        val name: String
    )

    class PluginSettingsGroup : HashMap<String, PluginSettings>()

    interface PluginSettings

    data class GcodeViewerSettings(
        val mobileSizeThreshold: Long,
        val sizeThreshold: Long
    ) : PluginSettings

    data class TradfriSettings(
        @SerializedName("selected_devices") val devices: List<TradfriPowerDevice>?
    ) : PluginSettings

    data class TuyaSettings(
        @SerializedName("arrSmartplugs") val devices: List<TuyaPowerDevice>?
    ) : PluginSettings

    data class TpLinkSmartPlugSettings(
        @SerializedName("arrSmartplugs") val devices: List<TpLinkSmartPlugPowerDevice>?
    ) : PluginSettings

    data class TasmotaSettings(
        @SerializedName("arrSmartplugs") val devices: List<TasmotaPowerDevice>?
    ) : PluginSettings

    data class GpioControlSettings(
        @SerializedName("gpio_configurations") val devices: List<GpioControlPowerDevice>?
    ) : PluginSettings

    data class OctoRelaySettings(
        // Attention! "devices" is used in PluginSettingsDeserializer, do not rename!
        val devices: List<OctoRelayPowerDevice>?
    ) : PluginSettings

    class WS281xSettings : PluginSettings

    class WledSettings : PluginSettings

    class OctoCamSettings : PluginSettings

    class MyStromSettings : PluginSettings

    data class OctoAppCompanionSettings(
        @SerializedName("encryptionKey") val encryptionKey: String?
    ) : PluginSettings

    data class MultiCamSettings(
        @SerializedName("multicam_profiles") val profiles: List<WebcamSettings>?
    ) : PluginSettings

    data class Discovery(
        @SerializedName("upnpUuid") val uuid: String?
    ) : PluginSettings

    data class UploadAnything(
        @SerializedName("allowed") val allowedExtensions: List<String>?
    ) : PluginSettings


    data class OctoEverywhere(
        @SerializedName("PrinterKey") val printerKey: String?
    ) : PluginSettings
}