package de.crysxd.octoapp.base.data.source

import android.content.Context
import de.crysxd.octoapp.base.ext.asStyleFileSize
import timber.log.Timber
import java.io.File
import java.io.InputStream
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.absoluteValue

class LocalMediaFileDataSource(context: Context) {

    companion object {
        private const val MAX_CACHE_SIZE = 128 * 1024 * 1024L // 128 MB
    }

    private val purgableCacheRoot = File(context.cacheDir, "purgable_media").also { it.mkdirs() }
    private val protectedCacheRoot = File(context.cacheDir, "media").also { it.mkdirs() }
    private val mutex = mutableMapOf<String, ReentrantLock>()

    fun load(url: String) = withMutexFor(url) {
        (getCacheFile(url, false).takeIf { it.exists() } ?: getCacheFile(url, true).takeIf { it.exists() })?.let {
            it.setLastModified(System.currentTimeMillis())
            Timber.i("Loading $url from cache")
            it
        }
    }

    fun store(url: String, inputStream: InputStream, canBeDeleted: Boolean = true) = withMutexFor(url) {
        getCacheFile(url, canBeDeleted).outputStream().use {
            Timber.i("Caching $url")
            inputStream.copyTo(it)
            inputStream.close()
            it.close()
            cleanUp()
            load(url)
        }
    }

    private fun <T> withMutexFor(url: String, block: () -> T) = mutex.getOrPut(url, { ReentrantLock() }).withLock(block)

    private fun getCacheFile(url: String, canBeDeleted: Boolean) =
        File(if (canBeDeleted) purgableCacheRoot else protectedCacheRoot, url.hashCode().absoluteValue.toString())

    fun cleanUp() {
        val files = (purgableCacheRoot.listFiles()?.map { Triple(it, it.length(), it.lastModified()) }?.sortedBy { it.third }?.toMutableList() ?: mutableListOf())
        var totalSize = files.sumOf { it.second }
        Timber.i("Total purgable cache size is ${totalSize.asStyleFileSize()}")
        if (totalSize > MAX_CACHE_SIZE) {
            Timber.i("Cache size of too large, maximum is ${MAX_CACHE_SIZE.asStyleFileSize()}. Cleaning up")
            // We use two as in extreme edge cases a timelapse and it's thumbnail could exceed the cache size but we need to keep both of them
            while (totalSize > MAX_CACHE_SIZE && files.size > 2) {
                val f = files.removeAt(0)
                Timber.d("Deleting ${f.first}")
                totalSize -= f.second
                f.first.delete()
            }
        }
    }
}