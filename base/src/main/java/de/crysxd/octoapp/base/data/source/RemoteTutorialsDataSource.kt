package de.crysxd.octoapp.base.data.source

import android.content.Context
import de.crysxd.octoapp.base.api.YoutubeApi
import de.crysxd.octoapp.base.data.models.YoutubePlaylist
import de.crysxd.octoapp.base.logging.TimberLogger
import de.crysxd.octoapp.octoprint.logging.LoggingInterceptorLogger
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.io.File
import java.util.Date
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

class RemoteTutorialsDataSource(context: Context) {

    private val logger = LoggingInterceptorLogger(TimberLogger(Logger.getLogger("YouTube/HTTP")).logger)
    private val cacheDir = File(context.cacheDir, "youtube-cache")
    private val cache = Cache(cacheDir, 1024 * 100L)
    private var forceNetworkOnNext = false
    private val client = OkHttpClient.Builder()
        .cache(cache)
        .addInterceptor(HttpLoggingInterceptor(logger).also { it.level = HttpLoggingInterceptor.Level.HEADERS })
        .addInterceptor {
            val builder = it.request().newBuilder()
            if (forceNetworkOnNext) {
                forceNetworkOnNext = false
                builder.cacheControl(CacheControl.FORCE_NETWORK)
            }
            it.proceed(builder.build())
        }.addNetworkInterceptor {
            Timber.v("Cache miss")
            it.proceed(it.request()).newBuilder()
                .addHeader("Cache-Control", "public, max-age=${TimeUnit.DAYS.toSeconds(1)}")
                .build()
        }.build()

    private val api = Retrofit.Builder()
        .client(client)
        .baseUrl("https://youtube.googleapis.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(YoutubeApi::class.java)

    suspend fun get(playlistId: String, skipCache: Boolean): List<YoutubePlaylist.PlaylistItem> {
        if (skipCache) {
            forceNetworkOnNext = true
        }

        Timber.i("Loading playlist $playlistId")

        val playlist = api.getPlaylist(playlistId)
        require(!playlist.items.isNullOrEmpty()) { "Playlist is empty" }

        return playlist.items.sortedByDescending {
            it.contentDetails?.videoPublishedAt ?: Date(0)
        }.filter {
            // Private videos have no publishing date
            it.contentDetails?.videoPublishedAt != null
        }
    }
}