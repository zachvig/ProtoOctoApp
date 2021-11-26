package de.crysxd.octoapp.octoprint.plugins.thespaghettidetective

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface SpaghettiDetectiveApi {

    @POST("plugin/thespaghettidetective")
    suspend fun getPluginStatus(@Body command: SpaghettiDetectiveCommand.GetPluginStatus = SpaghettiDetectiveCommand.GetPluginStatus): SpaghettiDetectiveStatus

    @GET("/_tsd_/webcam/{webcamIndex}/")
    suspend fun getSpaghettiCamFrame(@Path("webcamIndex") webcamIndex: Int = 0): SpaghettiCamFrame
}

class SpaghettiDetectiveApiWrapper(private val api: SpaghettiDetectiveApi) {

    suspend fun getLinkedPrinterId(): String? = api.getPluginStatus().linkedPrinter?.id

    suspend fun getSpaghettiCamFrameUrl(webcamIndex: Int = 0): String? = api.getSpaghettiCamFrame(webcamIndex).snapshot

}