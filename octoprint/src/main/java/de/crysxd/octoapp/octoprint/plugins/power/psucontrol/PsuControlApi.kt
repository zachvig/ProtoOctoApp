package de.crysxd.octoapp.octoprint.plugins.power.psucontrol

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface PsuControlApi {

    @POST("plugin/psucontrol")
    suspend fun sendPsuCommand(@Body command: PsuCommand): Response<Unit>

    @POST("plugin/psucontrol")
    suspend fun sendPsuCommandWithResponse(@Body command: PsuCommand): PsuControlResponse

}