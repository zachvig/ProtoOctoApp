package de.crysxd.octoapp.octoprint.models.settings

import com.google.gson.annotations.SerializedName
import de.crysxd.octoapp.octoprint.plugins.power.tasmota.TasmotaPowerDevice
import de.crysxd.octoapp.octoprint.plugins.power.tplinkplug.TpLinkSmartPlugPowerDevice
import de.crysxd.octoapp.octoprint.plugins.power.tradfri.TradfriPowerDevice


data class Settings(
    val webcam: WebcamSettings,
    val plugins: PluginSettingsGroup,
    val temperature: TemperatureSettings,
    val terminalFilters: List<TerminalFilter>
) {

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
        val mobileSizeThreshold: Int,
        val sizeThreshold: Int
    ) : PluginSettings

    data class TradfriSettings(
        @SerializedName("selected_devices") val devices: List<TradfriPowerDevice>
    ) : PluginSettings

    data class TpLinkSmartPlugSettings(
        @SerializedName("arrSmartplugs") val devices: List<TpLinkSmartPlugPowerDevice>
    ) : PluginSettings

    data class TasmotaSettings(
        @SerializedName("arrSmartplugs") val devices: List<TasmotaPowerDevice>
    ) : PluginSettings
}