package de.crysxd.octoapp.octoprint.api

import de.crysxd.octoapp.octoprint.models.printer.BedCommand
import de.crysxd.octoapp.octoprint.models.printer.PrinterState
import de.crysxd.octoapp.octoprint.models.printer.ToolCommand
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

    class Wrapper(private val wrapped: PrinterApi) {

        suspend fun getPrinterState(): PrinterState = wrapped.getPrinterState()

        suspend fun executeToolCommand(command: ToolCommand) {
            wrapped.executeToolCommand(command)
        }

        suspend fun executeBedCommand(command: BedCommand) {
            wrapped.executeBedCommand(command)
        }
    }
}