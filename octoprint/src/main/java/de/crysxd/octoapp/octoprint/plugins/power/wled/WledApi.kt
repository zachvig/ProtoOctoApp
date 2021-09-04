package de.crysxd.octoapp.octoprint.plugins.power.wled

import de.crysxd.octoapp.octoprint.plugins.power.tradfri.WledResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface WledApi {

    @POST("plugin/wled")
    suspend fun sendCommand(@Body command: WledCommand): Response<Unit>

    @GET("plugin/wled")
    suspend fun getStatus(): WledResponse

}