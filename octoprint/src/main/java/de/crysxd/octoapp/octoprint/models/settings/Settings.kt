package de.crysxd.octoapp.octoprint.models.settings

import com.google.gson.annotations.SerializedName
import de.crysxd.octoapp.octoprint.plugins.power.tradfri.TradfriPowerDevice


data class Settings(
    val webcam: WebcamSettings,
    val plugins: PluginSettingsGroup,
    val terminalFilters: List<TerminalFilter>
) {

    data class TerminalFilter(
        val name: String,
        val regex: String
    )

    data class PluginSettingsGroup(
        val settings: Map<String, PluginSettings>
    )

    interface PluginSettings

    data class GcodeViewerSettings(
        val mobileSizeThreshold: Int,
        val sizeThreshold: Int
    ) : PluginSettings

    data class TradfriSettings(
        @SerializedName("selected_devices") val devices: List<TradfriPowerDevice>
    ) : PluginSettings
}