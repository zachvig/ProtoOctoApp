package de.crysxd.octoapp.octoprint.api

import de.crysxd.octoapp.octoprint.models.printer.PrinterState
import retrofit2.http.GET


interface PrinterApi {

    @GET("printer")
    suspend fun getPrinterState(): PrinterState

}