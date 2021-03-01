package de.crysxd.octoapp.base.datasource

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import de.crysxd.octoapp.base.BuildConfig
import de.crysxd.octoapp.base.gcode.parse.models.Gcode
import de.crysxd.octoapp.base.gcode.parse.models.Layer
import de.crysxd.octoapp.base.gcode.parse.models.Move
import de.crysxd.octoapp.base.utils.measureTime
import de.crysxd.octoapp.octoprint.models.files.FileObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.nustaq.serialization.FSTConfiguration
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.*


private const val MAX_CACHE_SIZE = 128 * 1024 * 1024 // 128 MB

class LocalGcodeFileDataSource(
    context: Context,
    private val gson: Gson,
    private val sharedPreferences: SharedPreferences
) : GcodeFileDataSource {

    private val cacheRoot = File(context.cacheDir, "gcode2")
    private val oldCacheRoots = listOf(
        File(context.cacheDir, "gcode")
    )
    private val fstConfig = FSTConfiguration.createAndroidDefaultConfiguration()

    init {
        try {
            oldCacheRoots.filter { it.exists() }.forEach { it.deleteRecursively() }
        } catch (e: Exception) {
            Timber.e(e)
        }

        fstConfig.registerClass(Gcode::class.java)
        fstConfig.registerClass(Layer::class.java)
        fstConfig.registerClass(Move::class.java)
        fstConfig.registerClass(Move.Type::class.java)
    }

    override fun canLoadFile(file: FileObject.File): Boolean = getCacheEntry(file.cacheKey)?.let {
        // Cache hit if the file exists and the file was not changed at the server since it was cached
        it.localFile.exists() && it.fileDate == file.date
    } == true

    override fun loadFile(file: FileObject.File, allowLargeFileDownloads: Boolean): Flow<GcodeFileDataSource.LoadState> = flow {
        measureTime("Restore cache entry") {
            try {
                emit(GcodeFileDataSource.LoadState.Loading(0f))
                val cacheEntry = gson.fromJson(sharedPreferences.getString(file.cacheKey, null), CacheEntry::class.java)

                val gcode = try {
                    cacheEntry.localFile.inputStream().use {
                        fstConfig.decodeFromStream(it) as Gcode
                    }
                } catch (e: OutOfMemoryError) {
                    System.gc()
                    throw IOException(e)
                }

                emit(GcodeFileDataSource.LoadState.Ready(gcode))
            } catch (e: Exception) {
                // Cache most likely corrupted, clean out
                kotlin.runCatching {
                    sharedPreferences.edit { clear() }
                    cacheRoot.listFiles()?.forEach { it.delete() }
                }

                Timber.e(e)
                emit(GcodeFileDataSource.LoadState.Failed(e))
            }
        }
    }.flowOn(Dispatchers.IO)

    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun cacheGcode(file: FileObject.File, gcode: Gcode) = withContext(Dispatchers.IO) {
        // Delete old file exists
        getCacheEntry(file.cacheKey)?.localFile?.delete()

        Timber.i("Adding to cache: ${file.cacheKey}")
        val cacheEntry = CacheEntry(
            cachedAt = Date(),
            fileDate = file.date,
            localFile = createCacheFile()
        )

        cacheRoot.mkdirs()
        try {
            cacheEntry.localFile.outputStream().use {
                fstConfig.encodeToStream(it, gcode)
            }

            sharedPreferences.edit {
                putString(file.cacheKey, gson.toJson(cacheEntry))
            }
        } catch (e: OutOfMemoryError) {
            System.gc()
            throw IOException(e)
        }

        cleanUp()
    }

    private suspend fun cleanUp() = withContext(Dispatchers.IO) {
        fun totalSize() = cacheRoot.listFiles()?.sumByDouble { it.length().toDouble() }?.toLong() ?: 0L
        val cacheEntries = sharedPreferences.all.keys.map {
            Pair(it, getCacheEntry(it))
        }.sortedBy {
            it.second.cachedAt
        }.toMutableList()

        // Delete files until cache size is below max
        while (totalSize() > MAX_CACHE_SIZE) {
            if (BuildConfig.DEBUG) {
                Timber.i(
                    "Total size exceeds maximum: %.2f / %.2f Mb",
                    totalSize() / 1024f / 1024f,
                    MAX_CACHE_SIZE / 1024f / 1024f
                )
            }

            val oldest = cacheEntries.removeAt(0)
            Timber.i("Removing from cache: ${oldest.first}")
            sharedPreferences.edit { remove(oldest.first) }
            oldest.second.localFile.delete()

        }
    }

    private fun getCacheEntry(cacheKey: String) =
        gson.fromJson(sharedPreferences.getString(cacheKey, null), CacheEntry::class.java)

    private val FileObject.File.cacheKey get() = "$path@$date"

    private fun createCacheFile() = File(cacheRoot, UUID.randomUUID().toString())

    data class CacheEntry(
        val cachedAt: Date,
        val fileDate: Long,
        val localFile: File,
    )
}