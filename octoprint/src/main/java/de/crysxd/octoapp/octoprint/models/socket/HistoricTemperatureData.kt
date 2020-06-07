package de.crysxd.octoapp.octoprint.models.socket

import de.crysxd.octoapp.octoprint.models.printer.PrinterState

data class HistoricTemperatureData(
    val time: Long,
    val bed: PrinterState.ComponentTemperature,
    val tool0: PrinterState.ComponentTemperature
)