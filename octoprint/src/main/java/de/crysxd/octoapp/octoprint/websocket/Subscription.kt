package de.crysxd.octoapp.octoprint.websocket

internal data class Subscription(
    val state: State = State(),
    val plugins: List<String> = listOf(),
    val events: List<String> = listOf(
        "PrinterStateChanged",
        "PrinterProfileModified",
        "Connecting",
        "Connected",
        "Disconnected",
        "UpdatedFiles",
        "PrintStarted",
        "FileSelected",
        "PrintCancelling",
        "PrintCancelled",
        "PrintPausing",
        "PrintPaused",
        "PrintFailed",
        "FirmwareData",
        "SettingsUpdated",
        "MovieRendering",
        "MovieDone",
        "MovieFailed",
    ),
) {
    data class State(
        val logs: Any = false,
        val messages: Boolean = false,
    )
}
