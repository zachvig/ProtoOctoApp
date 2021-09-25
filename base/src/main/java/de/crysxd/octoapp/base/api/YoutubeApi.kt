package de.crysxd.octoapp.base.api

import de.crysxd.octoapp.base.data.models.YoutubePlaylist
import retrofit2.http.GET
import retrofit2.http.Query

interface YoutubeApi {

    companion object {
        private const val API_KEY = "AIzaSyA8jA_80skPz8jC6AEaa-UQtOdmuTLLpCo"
    }

    @GET("youtube/v3/playlistItems?part=snippet%2CcontentDetails&maxResults=100")
    suspend fun getPlaylist(@Query("playlistId") playlistId: String, @Query("key") apiKey: String = API_KEY): YoutubePlaylist
}
