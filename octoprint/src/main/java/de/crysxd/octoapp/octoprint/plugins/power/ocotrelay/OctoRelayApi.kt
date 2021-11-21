package de.crysxd.octoapp.octoprint.plugins.power.ocotrelay

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface OctoRelayApi {

    @POST("plugin/octorelay")
    suspend fun sendCommand(@Body command: OctoRelayCommand): Response<OctoRelayResponse>

}