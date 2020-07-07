package de.crysxd.octoapp.octoprint.api

import de.crysxd.octoapp.octoprint.models.printer.*
import de.crysxd.octoapp.octoprint.models.socket.HistoricTemperatureData
import de.crysxd.octoapp.octoprint.websocket.EventWebSocket
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST


interface PrinterApi {

    @GET("printer")
    suspend fun getPrinterState(): PrinterState

    @POST("printer/tool")
    // Body needs to be Any in order to trick Gson to serialize all fields
    suspend fun executeToolCommand(@Body toolCommand: Any): Response<Unit>

    @POST("printer/bed")
    // Body needs to be Any in order to trick Gson to serialize all fields
    suspend fun executeBedCommand(@Body bedCommand: Any): Response<Unit>

    @POST("printer/printhead")
    // Body needs to be Any in order to trick Gson to serialize all fields
    suspend fun executePrintHeadCommand(@Body printHeadCommand: Any): Response<Unit>

    @POST("printer/command")
    // Body needs to be Any in order to trick Gson to serialize all fields
    suspend fun executeGcodeCommand(@Body gcodeCommand: Any): Response<Unit>

    class Wrapper(private val wrapped: PrinterApi, private val webSocket: EventWebSocket) {

        suspend fun getPrinterState(): PrinterState = wrapped.getPrinterState()

        suspend fun executeToolCommand(command: ToolCommand) {
            when (command) {
                is ToolCommand.SetTargetTemperatureToolCommand -> postInterpolatedMessage(toolTargetTemp = command.targets.tool0.toFloat())
                is ToolCommand.SetTemperatureOffsetToolCommand -> postInterpolatedMessage(bedOffset = command.offsets.tool0.toFloat())
            }

            wrapped.executeToolCommand(command)
        }

        suspend fun executeBedCommand(command: BedCommand) {
            when (command) {
                is BedCommand.SetTargetTemperatureToolCommand -> postInterpolatedMessage(bedTargetTemp = command.target.toFloat())
                is BedCommand.SetTemperatureOffsetToolCommand -> postInterpolatedMessage(bedOffset = command.offset.toFloat())
            }

            wrapped.executeBedCommand(command)
        }

        suspend fun executePrintHeadCommand(command: PrintHeadCommand) {
            wrapped.executePrintHeadCommand(command)
        }

        suspend fun executeGcodeCommand(command: GcodeCommand) {
            wrapped.executeGcodeCommand(command)
        }

        private fun postInterpolatedMessage(toolTargetTemp: Float? = null, toolOffset: Float? = null, bedTargetTemp: Float? = null, bedOffset: Float? = null) =
            webSocket.postCurrentMessageInterpolation {
                val lastTemps = it.temps.firstOrNull()
                val tool0 = PrinterState.ComponentTemperature(
                    target = toolTargetTemp ?: lastTemps?.tool0?.target ?: 0f,
                    offset = toolOffset ?: lastTemps?.tool0?.offset ?: 0f,
                    actual = lastTemps?.tool0?.actual ?: 0f
                )
                val bed = PrinterState.ComponentTemperature(
                    target = bedTargetTemp ?: lastTemps?.bed?.target ?: 0f,
                    offset = bedOffset ?: lastTemps?.bed?.offset ?: 0f,
                    actual = lastTemps?.bed?.actual ?: 0f
                )
                val temps = it.temps.toMutableList().also { l -> l.add(HistoricTemperatureData(System.currentTimeMillis(), tool0, bed)) }
                it.copy(temps = temps)
            }
    }
}