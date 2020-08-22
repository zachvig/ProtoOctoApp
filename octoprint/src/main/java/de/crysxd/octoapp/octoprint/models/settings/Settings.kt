package de.crysxd.octoapp.octoprint.models.settings

data class Settings(
    val webcam: WebcamSettings,
    val plugins: Map<String, Map<String, *>>,
    val terminalFilters: List<TerminalFilter>
) {

    data class TerminalFilter(
        val name: String,
        val regex: String
    )
}