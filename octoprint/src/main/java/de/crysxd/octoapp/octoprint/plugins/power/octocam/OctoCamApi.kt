package de.crysxd.octoapp.octoprint.plugins.power.octocam

import retrofit2.http.Body
import retrofit2.http.POST

interface OctoCamApi {

    @POST("plugin/octocam")
    suspend fun sendCommand(@Body command: OctoCamCommand?): OctoCamResponse

}