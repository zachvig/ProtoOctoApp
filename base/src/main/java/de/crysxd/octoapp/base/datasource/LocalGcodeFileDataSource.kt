package de.crysxd.octoapp.base.datasource

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import de.crysxd.octoapp.base.BuildConfig
import de.crysxd.octoapp.base.gcode.parse.models.Gcode
import de.crysxd.octoapp.base.gcode.parse.models.Layer
import de.crysxd.octoapp.base.gcode.parse.models.LayerInfo
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
) {

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
        fstConfig.registerClass(LayerInfo::class.java)
        fstConfig.registerClass(Move::class.java)
        fstConfig.registerClass(Move.Type::class.java)
    }

    fun canLoadFile(file: FileObject.File): Boolean = getCacheEntry(file.cacheKey)?.let {
        // Cache hit if the file exists and the file was not changed at the server since it was cached
        file.cacheKey.cacheDir.exists() && file.cacheKey.indexFile.exists() && it.fileDate == file.date
    } == true

    fun loadFile(file: FileObject.File): Flow<GcodeFileDataSource.LoadState> = flow {
        measureTime("Restore cache entry") {
            try {
                emit(GcodeFileDataSource.LoadState.Loading(0f))

                val gcode = try {
                    file.cacheKey.indexFile.inputStream().use {
                        fstConfig.decodeFromStream(it) as Gcode
                    }
                } catch (e: OutOfMemoryError) {
                    throw IOException(e)
                }

                emit(GcodeFileDataSource.LoadState.Ready(gcode))
            } catch (e: Exception) {
                // Cache most likely corrupted, clean out
                removeFromCache(file)
                Timber.e(e)
                emit(GcodeFileDataSource.LoadState.Failed(e))
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun loadLayer(cacheKey: CacheKey, layerInfo: LayerInfo) = withContext(Dispatchers.IO) {
        try {
            File(cacheKey.cacheDir, layerInfo.positionInFile.toString()).inputStream().use {
                fstConfig.decodeFromStream(it) as Layer
            }
        } catch (e: Exception) {
            removeFromCache(cacheKey)
            Timber.e(e)
            throw e
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    internal suspend fun cacheGcodeLayer(file: FileObject.File, layer: Layer) = withContext(Dispatchers.IO) {
        measureTime("Cache layer of ${file.cacheKey} at ${layer.info.zHeight}mm") {
            try {
                val dir = file.cacheKey.cacheDir
                dir.mkdirs()
                val f = File(dir, layer.info.positionInFile.toString())
                Timber.v("Caching layer to $f")
                f.outputStream().use {
                    fstConfig.encodeToStream(it, layer)
                }
            } catch (e: OutOfMemoryError) {
                System.gc()
                throw IOException(e)
            }
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun cacheGcode(file: FileObject.File, gcode: Gcode): Gcode = withContext(Dispatchers.IO) {
        // Delete old file exists
        val upgradedGcode = gcode.copy(cacheKey = file.cacheKey)
        Timber.i("Adding to cache: ${file.cacheKey}")
        createCacheEntry(file)
        file.cacheKey.cacheDir.mkdirs()
        file.cacheKey.indexFile.createNewFile()

        try {
            file.cacheKey.indexFile.outputStream().use {
                fstConfig.encodeToStream(it, upgradedGcode)
            }
        } catch (e: OutOfMemoryError) {
            System.gc()
            removeFromCache(file)
            throw IOException(e)
        }

        cleanUp()
        upgradedGcode
    }

    internal fun removeFromCache(file: FileObject.File) {
        removeFromCache(file.cacheKey)
    }

    private fun removeFromCache(cacheKey: CacheKey) {
        cacheKey.cacheDir.deleteRecursively()
        sharedPreferences.edit { remove(cacheKey) }
    }

    private fun createCacheEntry(file: FileObject.File): CacheEntry {
        val cacheEntry = CacheEntry(
            cachedAt = Date(),
            fileDate = file.date,
        )

        sharedPreferences.edit {
            putString(file.cacheKey, gson.toJson(cacheEntry))
        }

        return cacheEntry
    }


    private suspend fun cleanUp() = withContext(Dispatchers.IO) {
        fun totalSize() = cacheRoot.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
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
            removeFromCache(oldest.first)
        }
    }

    private fun getCacheEntry(cacheKey: String) =
        gson.fromJson(sharedPreferences.getString(cacheKey, null), CacheEntry::class.java)

    private val FileObject.File.cacheKey: CacheKey get() = "$path:$date".hashCode().toString()

    private val CacheKey.cacheDir get() = File(cacheRoot, this)

    private val CacheKey.indexFile get() = File(cacheDir, "index")

    data class CacheEntry(
        val cachedAt: Date,
        val fileDate: Long,
    )
}

typealias CacheKey = String

