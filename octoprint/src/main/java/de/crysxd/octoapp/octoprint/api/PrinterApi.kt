package de.crysxd.octoapp.octoprint.api

import de.crysxd.octoapp.octoprint.models.printer.*
import de.crysxd.octoapp.octoprint.websocket.EventWebSocket
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST


interface PrinterApi {

    @GET("printer")
    suspend fun getPrinterState(): PrinterState

    @POST("printer/chamber")
    // Body needs to be Any in order to trick Gson to serialize all fields
    suspend fun executeChamberCommand(@Body chamberCommand: Any): Response<Unit>

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

    class Wrapper(private val wrapped: PrinterApi) {

        suspend fun getPrinterState(): PrinterState = wrapped.getPrinterState()

        suspend fun executeChamberCommand(chamberCommand: ChamberCommand) {
            wrapped.executeChamberCommand(chamberCommand)
        }

        suspend fun executeToolCommand(command: ToolCommand) {
            wrapped.executeToolCommand(command)
        }

        suspend fun executeBedCommand(command: BedCommand) {
            wrapped.executeBedCommand(command)
        }

        suspend fun executePrintHeadCommand(command: PrintHeadCommand) {
            wrapped.executePrintHeadCommand(command)
        }

        suspend fun executeGcodeCommand(command: GcodeCommand) {
            wrapped.executeGcodeCommand(command)
        }
    }
}