package de.crysxd.octoapp.octoprint.models.socket

import de.crysxd.octoapp.octoprint.models.printer.PrinterState

data class HistoricTemperatureData(
    val time: Long,
    val components: Map<String, PrinterState.ComponentTemperature>
)