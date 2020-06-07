package de.crysxd.octoapp.octoprint.models.socket

import com.google.gson.JsonObject
import de.crysxd.octoapp.octoprint.models.files.FileOrigin
import de.crysxd.octoapp.octoprint.models.printer.PrinterState

sealed class Message {

    data class ConnectedMessage(
        val version: String,
        val displayVersion: String
    ) : Message()

    data class CurrentMessage(
        val logs: List<String>,
        val temps: List<HistoricTemperatureData>,
        val state: PrinterState.State?
    ) : Message()

    data class PluginMessage(
        val data: JsonObject
    ) : Message()

    data class RawMessage(
        val rawData: JsonObject
    ) : Message()

    sealed class EventMessage() : Message() {

        abstract class FileMessageEvent(
            val origin: FileOrigin,
            val name: String,
            val path: String
        ) : EventMessage()

        data class PrinterStateChanged(
            val state: String,
            val stateId: PrinterState
        ) : EventMessage() {

            enum class PrinterState {
                OFFLINE, CONNECTING, OPEN_SERIAL, OPERATIONAL, STARTING, PRINTING, CANCELLING, UNKNOWN
            }
        }

        object Connecting : EventMessage()

        data class Connected(
            val baudrate: Int,
            val port: String
        ) : EventMessage()

        class PrintStarted(
            origin: FileOrigin,
            name: String,
            path: String
        ) : FileMessageEvent(origin, name, path)

        class FileSelected(
            origin: FileOrigin,
            name: String,
            path: String
        ): FileMessageEvent(origin, name, path)

        class PrintPausing(
            origin: FileOrigin,
            name: String,
            path: String
        ): FileMessageEvent(origin, name, path)

        class PrintPaused(
            origin: FileOrigin,
            name: String,
            path: String
        ): FileMessageEvent(origin, name, path)

        class PrintCancelling(
            origin: FileOrigin,
            name: String,
            path: String
        ): FileMessageEvent(origin, name, path)

        class PrintCancelled(
            origin: FileOrigin,
            name: String,
            path: String
        ): FileMessageEvent(origin, name, path)

        class PrintFailed(
            origin: FileOrigin,
            name: String,
            path: String
        ) :FileMessageEvent(origin, name, path)

        object Disconnected : EventMessage()

        object Unknown : EventMessage()

    }
}