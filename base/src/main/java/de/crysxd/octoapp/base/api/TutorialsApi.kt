package de.crysxd.octoapp.base.api

import de.crysxd.octoapp.base.data.models.YoutubePlaylist
import retrofit2.http.GET

interface TutorialsApi {

    @GET("config/tutorials.json")
    suspend fun getPlaylist(): YoutubePlaylist
}
