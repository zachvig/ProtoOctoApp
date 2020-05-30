package de.crysxd.octoapp.octoprint.models.printer

data class PrinterState(
    val text: String,
    val flags: Flags
) {

    data class Flags(
        val operational: Boolean,
        val paused: Boolean,
        val printing: Boolean,
        val cancelling: Boolean,
        val pausing: Boolean,
        val sdReady: Boolean,
        val error: Boolean,
        val ready: Boolean,
        val closedOrError: Boolean
    )
}