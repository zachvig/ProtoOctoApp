package de.crysxd.octoapp.octoprint.api

import de.crysxd.octoapp.octoprint.models.psu.PsuCommand
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST


interface PsuApi {

    @POST("plugin/psucontrol")
    suspend fun sendPsuCommand(@Body command: PsuCommand): Response<Unit>

    class PsuApiWrapper(private val wrapped: PsuApi) {

        suspend fun turnPsuOn() {
            wrapped.sendPsuCommand(PsuCommand.TurnOnPsuCommand)
        }

        suspend fun turnPsuOff() {
            wrapped.sendPsuCommand(PsuCommand.TurnOffPsuCommand)
        }
    }
}