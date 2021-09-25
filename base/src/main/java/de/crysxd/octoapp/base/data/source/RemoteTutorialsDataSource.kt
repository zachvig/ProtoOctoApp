package de.crysxd.octoapp.base.data.source

import de.crysxd.octoapp.base.api.YoutubeApi
import de.crysxd.octoapp.base.data.models.YoutubePlaylist
import de.crysxd.octoapp.base.logging.TimberLogger
import de.crysxd.octoapp.octoprint.logging.LoggingInterceptorLogger
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.util.Date
import java.util.logging.Logger

class RemoteTutorialsDataSource {

    suspend fun get(playlistId: String): List<YoutubePlaylist.PlaylistItem> {
        Timber.i("Loading playlist $playlistId")
        val logger = LoggingInterceptorLogger(TimberLogger(Logger.getLogger("YouTube/HTTP")).logger)

        val client = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor(logger).also { it.level = HttpLoggingInterceptor.Level.HEADERS })
            .build()

        val playlist = Retrofit.Builder()
            .client(client)
            .baseUrl("https://youtube.googleapis.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(YoutubeApi::class.java)
            .getPlaylist(playlistId)

        require(!playlist.items.isNullOrEmpty()) { "Playlist is empty" }

        return playlist.items.sortedByDescending {
            it.contentDetails?.videoPublishedAt ?: Date(0)
        }.filter {
            // Private videos have no publishing date
            it.contentDetails?.videoPublishedAt != null
        }
    }
}