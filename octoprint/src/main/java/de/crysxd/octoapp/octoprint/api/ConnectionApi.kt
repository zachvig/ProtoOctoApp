package de.crysxd.octoapp.octoprint.api

import de.crysxd.octoapp.octoprint.models.connection.ConnectionCommand
import de.crysxd.octoapp.octoprint.models.connection.ConnectionResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ConnectionApi {

    @GET("connection")
    suspend fun getConnection(): ConnectionResponse

    @POST("connection")
    suspend fun executeConnectionCommand(@Body command: Any): Response<Unit>

    class Wrapper(private val wrapped: ConnectionApi) {

        suspend fun getConnection(): ConnectionResponse = wrapped.getConnection()

        suspend fun executeConnectionCommand(command: ConnectionCommand) {
            wrapped.executeConnectionCommand(command)
        }
    }
}