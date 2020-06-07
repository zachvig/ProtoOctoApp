package de.crysxd.octoapp.octoprint.models.socket

import com.google.gson.JsonObject
import de.crysxd.octoapp.octoprint.models.printer.PrinterState

sealed class Message {

    data class ConnectedMessage(
        val version: String,
        val displayVersion: String
    ) : Message()

    data class CurrentMessage(
        val logs: List<String>,
        val temps: PrinterState.PrinterTemperature,
        val state: PrinterState
    ) : Message()

    data class PluginMessage(
        val data: JsonObject
    ) : Message()

    data class RawMessage(
        val rawData: JsonObject
    ) : Message()
}