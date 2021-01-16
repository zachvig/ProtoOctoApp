package de.crysxd.octoapp.octoprint.plugins.power.tasmota

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface TasmotaApi {

    @POST("plugin/tplinksmartplug")
    suspend fun sendCommand(@Body command: TasmotaCommand): Response<Unit>

    @POST("plugin/tplinksmartplug")
    suspend fun sendCommandWithResponse(@Body command: TasmotaCommand): TasmotaResponse

}