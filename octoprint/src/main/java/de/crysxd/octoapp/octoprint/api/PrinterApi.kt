package de.crysxd.octoapp.octoprint.api

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
    suspend fun executeToolCommand(@Body toolCommand: Any): Response<Unit>

    class Wrapper(private val wrapped: PrinterApi) {

        suspend fun getPrinterState(): PrinterState = wrapped.getPrinterState()

        suspend fun executeToolCommand(command: ToolCommand) {
            wrapped.executeToolCommand(command)
        }
    }
}