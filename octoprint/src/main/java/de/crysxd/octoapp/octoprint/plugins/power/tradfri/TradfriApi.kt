package de.crysxd.octoapp.octoprint.plugins.power.tradfri

import de.crysxd.octoapp.octoprint.plugins.power.psucontrol.TradfriResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface TradfriApi {

    @POST("plugin/ikea_tradfri")
    suspend fun sendCommand(@Body command: TradfriCommand): Response<Unit>

    @POST("plugin/ikea_tradfri")
    suspend fun sendCommandWithResponse(@Body command: TradfriCommand): TradfriResponse

}