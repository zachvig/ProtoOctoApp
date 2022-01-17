package de.crysxd.octoapp.octoprint.plugins.power.wemoswitch

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface WemoSwitchApi {

    @POST("plugin/wemoswitch")
    suspend fun sendCommand(@Body command: WemoSwitchCommand): Response<Unit>

    @POST("plugin/wemoswitch")
    suspend fun sendCommandWithResponse(@Body command: WemoSwitchCommand): WemoSwitchResponse?

}