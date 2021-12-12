package de.crysxd.octoapp.octoprint.plugins.power.tradfri

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface TuyaApi {

    @POST("plugin/tuyasmartplug")
    suspend fun sendCommand(@Body command: TuyaCommand): Response<Unit>

    @POST("plugin/tuyasmartplug")
    suspend fun sendCommandWithResponse(@Body command: TuyaCommand): Response<TuyaResponse?>

}