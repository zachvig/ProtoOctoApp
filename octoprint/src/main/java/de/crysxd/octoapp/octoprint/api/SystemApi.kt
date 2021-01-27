package de.crysxd.octoapp.octoprint.api

import de.crysxd.octoapp.octoprint.models.system.SystemCommand
import de.crysxd.octoapp.octoprint.models.system.SystemCommandList
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface SystemApi {

    @GET("system/commands")
    suspend fun getSystemCommands(): SystemCommandList

    @POST("system/commands/{source}/{action}")
    suspend fun executeSystemCommand(@Path("source") source: String, @Path("action") action: String): Response<Unit>

    class Wrapper(private val wrapped: SystemApi) {

        suspend fun getSystemCommands() = wrapped.getSystemCommands()

        suspend fun executeSystemCommand(cmd: SystemCommand) = wrapped.executeSystemCommand(source = cmd.source, action = cmd.action)
    }
}