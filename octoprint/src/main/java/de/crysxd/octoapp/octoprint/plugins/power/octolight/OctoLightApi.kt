package de.crysxd.octoapp.octoprint.plugins.power.octolight

import retrofit2.http.GET

interface OctoLightApi {

    @GET("plugin/octolight")
    suspend fun toggleLight()

}