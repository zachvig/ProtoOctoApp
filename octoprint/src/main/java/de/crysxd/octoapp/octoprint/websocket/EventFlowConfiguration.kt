package de.crysxd.octoapp.octoprint.websocket

data class EventFlowConfiguration(
    // Default throttle is 1 -> 500ms target interval
    val throttle: Int = 1,

    // Default logs we always want to receive -> TuneWidget
    // This allows the live notification and other parts of the app to look out for those values so the TuneWidget gets them right away
    val requestTerminalLogs: List<String> = listOf(
        "M106",
        "M107",
        "M220",
        "M221",
        "M290",
        "Probe Offset"
    ),
) {
    companion object {
        const val ALL_LOGS = "%%ALL_LOGS%%"
    }
}