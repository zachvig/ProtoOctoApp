package de.crysxd.octoapp.base.data.source

import de.crysxd.octoapp.base.data.repository.OctoPrintRepository
import de.crysxd.octoapp.base.ext.await
import de.crysxd.octoapp.base.logging.TimberLogger
import de.crysxd.octoapp.base.network.OctoPrintProvider
import de.crysxd.octoapp.octoprint.logging.LoggingInterceptorLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import timber.log.Timber
import java.util.logging.Logger

class RemoteMediaFileDataSource(
    private val octoPrintRepository: OctoPrintRepository,
    private val octoPrintProvider: OctoPrintProvider,
) {

    suspend fun load(url: String) = withContext(Dispatchers.IO) {
        Timber.i("Loading $url from server")
        val logger = TimberLogger(Logger.getLogger("RemoteMediaDataSource")).logger
        val okHttp = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor(LoggingInterceptorLogger(logger)).setLevel(HttpLoggingInterceptor.Level.BASIC))
            .build()
        return@withContext doLoad(url, okHttp)
    }

    fun ensureAbsoluteUrl(url: String?): String {
        val resolvedUrl = requireNotNull(url).toHttpUrlOrNull() ?: octoPrintRepository.getActiveInstanceSnapshot()?.webUrl?.resolve(url)
        return requireNotNull(resolvedUrl).toString()
    }

    suspend fun loadFromOctoPrint(url: String) = withContext(Dispatchers.IO) {
        Timber.i("Loading $url from server")
        return@withContext doLoad(url, octoPrintProvider.octoPrint().createOkHttpClient())
    }

    private suspend fun doLoad(url: String, okHttpClient: OkHttpClient) =
        okHttpClient.newCall(Request.Builder().get().url(url).build()).await().body?.byteStream() ?: throw IllegalStateException("No body")

}