package de.crysxd.octoapp.octoprint.plugins.thespaghettidetective

import retrofit2.http.Body
import retrofit2.http.POST

interface SpaghettiDetectiveApi {

    @POST("plugin/thespaghettidetective")
    suspend fun getPluginStatus(@Body command: SpaghettiDetectiveCommand.GetPluginStatus = SpaghettiDetectiveCommand.GetPluginStatus): SpaghettiDetectiveStatus
}

class SpaghettiDetectiveApiWrapper(private val api: SpaghettiDetectiveApi) {

    suspend fun getLinkedPrinterId(): String? = api.getPluginStatus().linkedPrinter?.id

}