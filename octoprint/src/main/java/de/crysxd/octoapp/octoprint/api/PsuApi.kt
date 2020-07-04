package de.crysxd.octoapp.octoprint.api

import de.crysxd.octoapp.octoprint.models.psu.PsuCommand
import de.crysxd.octoapp.octoprint.models.socket.Message
import de.crysxd.octoapp.octoprint.websocket.EventWebSocket
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST


interface PsuApi {

    @POST("plugin/psucontrol")
    suspend fun sendPsuCommand(@Body command: PsuCommand): Response<Unit>

    class Wrapper(private val wrapped: PsuApi, private val webSocket: EventWebSocket) {

        suspend fun turnPsuOn() {
            webSocket.postMessage(Message.PsuControlPluginMessage(true))
            wrapped.sendPsuCommand(PsuCommand.TurnOnPsuCommand)
        }

        suspend fun turnPsuOff() {
            webSocket.postMessage(Message.PsuControlPluginMessage(false))
            webSocket.postMessage(Message.EventMessage.Disconnected)
            wrapped.sendPsuCommand(PsuCommand.TurnOffPsuCommand)
        }
    }
}