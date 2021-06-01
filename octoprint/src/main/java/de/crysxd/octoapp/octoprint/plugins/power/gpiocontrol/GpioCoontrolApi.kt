package de.crysxd.octoapp.octoprint.plugins.power.gpiocontrol

import de.crysxd.octoapp.octoprint.plugins.power.tasmota.TasmotaCommand
import de.crysxd.octoapp.octoprint.plugins.power.tasmota.TasmotaResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface GpioCoontrolApi {

    @POST("plugin/gpiocontrol")
    suspend fun sendCommand(@Body command: GpioControlCommand): Response<Unit>

    @GET("plugin/gpiocontrol")
    suspend fun getGpioState(): Array<GpioState>

}