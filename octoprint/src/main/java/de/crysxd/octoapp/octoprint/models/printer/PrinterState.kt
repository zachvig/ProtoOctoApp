package de.crysxd.octoapp.octoprint.models.printer

data class PrinterState(
    val state: State?,
    val temperature: PrinterTemperature?
) {

    data class State(
        val text: String,
        val flags: Flags
    )

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
    ) {
        fun isPrinting() = listOf(printing, paused, pausing, cancelling).any { it }
        fun isOperational() = listOf(operational).any { it }
        fun isError() = listOf(error, closedOrError).any { it }
    }

    data class PrinterTemperature(
        val bed: ComponentTemperature,
        val tool0: ComponentTemperature
    )

    data class ComponentTemperature(
        val actual: Float,
        val target: Float
    )
}