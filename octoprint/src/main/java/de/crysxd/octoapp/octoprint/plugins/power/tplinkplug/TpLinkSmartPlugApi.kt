package de.crysxd.octoapp.octoprint.plugins.power.tplinkplug

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface TpLinkSmartPlugApi {

    @POST("plugin/tplinksmartplug")
    suspend fun sendCommand(@Body command: TpLinkSmartPlugCommand): Response<Unit>

    @POST("plugin/tplinksmartplug")
    suspend fun sendCommandWithResponse(@Body command: TpLinkSmartPlugCommand): TpLinkSmartPlugResponse

}