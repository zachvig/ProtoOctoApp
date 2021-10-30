package de.crysxd.octoapp.base.data.source

import de.crysxd.octoapp.base.ext.await
import de.crysxd.octoapp.base.logging.TimberLogger
import de.crysxd.octoapp.octoprint.logging.LoggingInterceptorLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import java.util.logging.Logger

class RemoteMediaFileDataSource {

    suspend fun load(url: String) = withContext(Dispatchers.IO) {
        val logger = TimberLogger(Logger.getLogger("RemoteMediaDataSource")).logger
        val okHttp = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor(LoggingInterceptorLogger(logger)).setLevel(HttpLoggingInterceptor.Level.BASIC))
            .build()
        val request = Request.Builder().get().url(url).build()
        okHttp.newCall(request).await().body?.byteStream() ?: throw IllegalStateException("No body")
    }
}