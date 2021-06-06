package de.crysxd.octoapp.octoprint.plugins.power.mystrom

import de.crysxd.octoapp.octoprint.plugins.power.gpiocontrol.GpioState
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface MyStromApi {

    @POST("plugin/mystromswitch")
    suspend fun sendCommand(@Body command: MyStromCommand): Response<Unit>

    @GET("plugin/mystromswitch")
    suspend fun getGpioState()
}