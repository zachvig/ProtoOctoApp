package de.crysxd.octoapp.octoprint.api

import retrofit2.http.GET

interface ProbeApi {

    @GET("/")
    suspend fun probe()

}