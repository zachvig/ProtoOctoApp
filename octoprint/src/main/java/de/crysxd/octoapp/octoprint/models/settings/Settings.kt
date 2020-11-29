package de.crysxd.octoapp.octoprint.models.settings


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
        val settings: Map<String, Settings.PluginSettings>
    )

    interface PluginSettings

    data class GcodeViewerSettings(
        val mobileSizeThreshold: Int,
        val sizeThreshold: Int
    ) : PluginSettings
}