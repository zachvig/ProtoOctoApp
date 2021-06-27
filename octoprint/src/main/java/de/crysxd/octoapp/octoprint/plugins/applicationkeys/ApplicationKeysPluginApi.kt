package de.crysxd.octoapp.octoprint.plugins.applicationkeys

import retrofit2.Response
import retrofit2.http.GET

interface ApplicationKeysPluginApi {

    @GET("plugin/appkeys/probe")
    suspend fun probe(): Response<Unit>

    class Wrapper(private val wrapped: ApplicationKeysPluginApi) {

        suspend fun probe() = try {
            wrapped.probe().code() == 204
        } catch (e: Exception) {
            false
        }
    }
}