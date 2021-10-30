package de.crysxd.octoapp.base.data.source

import android.content.Context
import androidx.core.net.toUri
import timber.log.Timber
import java.io.File
import java.io.InputStream
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.absoluteValue

class LocalMediaFileDataSource(context: Context) {

    private val cacheRoot = File(context.cacheDir, "media").also {
        it.mkdirs()
    }

    private val mutex = mutableMapOf<String, ReentrantLock>()

    fun load(url: String) = withMutexFor(url) {
        getCacheFile(url).takeIf { it.exists() }?.let {
            Timber.i("Loading $url from cache")
            it.toUri()
        }
    }

    fun store(url: String, inputStream: InputStream) = withMutexFor(url) {
        getCacheFile(url).outputStream().use {
            Timber.i("Caching $url")
            inputStream.copyTo(it)
            inputStream.close()
            it.close()
            load(url)
        }
    }

    private fun <T> withMutexFor(url: String, block: () -> T) = mutex.getOrPut(url, { ReentrantLock() }).withLock(block)

    private fun getCacheFile(url: String) = File(cacheRoot, url.hashCode().absoluteValue.toString())

}