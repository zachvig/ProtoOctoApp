package de.crysxd.octoapp.octoprint.plugins.octoeverywhere

import retrofit2.http.GET

interface OctoEverywhereApi {

    @GET("plugin/octoeverywhere")
    suspend fun getInfo(): OctoEverywhereInfo
}