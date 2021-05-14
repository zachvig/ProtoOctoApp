package de.crysxd.octoapp.octoprint.plugins.power.ws281x

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface WS281xApi {

    @POST("plugin/ws281x_led_status")
    suspend fun sendCommand(@Body command: WS281xCommand)

    @GET("plugin/ws281x_led_status")
    suspend fun getStatus(): WS281xResponse

}