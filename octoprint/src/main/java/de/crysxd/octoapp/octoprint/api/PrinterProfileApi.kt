package de.crysxd.octoapp.octoprint.api

import de.crysxd.octoapp.octoprint.models.profiles.PrinterProfiles
import retrofit2.http.GET

interface PrinterProfileApi {

    @GET("printerprofiles")
    suspend fun getPrinterProfiles(): PrinterProfiles

}