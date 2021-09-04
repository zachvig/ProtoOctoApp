package de.crysxd.octoapp.base.datasource

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import de.crysxd.octoapp.base.BuildConfig
import de.crysxd.octoapp.base.ext.asStyleFileSize
import de.crysxd.octoapp.base.gcode.parse.models.Gcode
import de.crysxd.octoapp.base.gcode.parse.models.Layer
import de.crysxd.octoapp.base.gcode.parse.models.LayerInfo
import de.crysxd.octoapp.base.gcode.parse.models.Move
import de.crysxd.octoapp.octoprint.models.files.FileObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import org.nustaq.serialization.FSTConfiguration
import timber.log.Timber
import java.io.*
import kotlin.math.absoluteValue


private const val MAX_CACHE_SIZE = 128 * 1024 * 1024L // 128 MB

class LocalGcodeFileDataSource(
    context: Context,
    private val gson: Gson,
    private val sharedPreferences: SharedPreferences
) {

    private val cacheRoot = File(context.cacheDir, "gcode4")
    private val oldCacheRoots = listOf(
        File(context.cacheDir, "gcode3"),
        File(context.cacheDir, "gcode2"),
        File(context.cacheDir, "gcode"),
        File(File(context.cacheDir.parentFile, "shared_prefs"), "gcode_cache_index_1"),
        File(File(context.cacheDir.parentFile, "shared_prefs"), "gcode_cache_index_2"),
    )
    private lateinit var fstConfig: FSTConfiguration
    private val initJob = GlobalScope.launch(Dispatchers.IO) {
        fstConfig = FSTConfiguration.createAndroidDefaultConfiguration()
        fstConfig.registerClass(Gcode::class.java)
        fstConfig.registerClass(LayerInfo::class.java)
        fstConfig.registerClass(Move::class.java)
        fstConfig.registerClass(Move.Type::class.java)

        try {
            oldCacheRoots.filter { it.exists() }.forEach { it.deleteRecursively() }
        } catch (e: Exception) {
            Timber.e(e)
        }

        cacheRoot.mkdirs()
    }

    fun canLoadFile(file: FileObject.File): Boolean {
        val cacheKey = getCacheKey(file)
        val cached = getCacheEntry(cacheKey)?.let {
            // Cache hit if the file exists and the file was not changed at the server since it was cached
            getDataFile(cacheKey).exists() && getIndexFile(cacheKey).exists() && it.fileDate == file.date
        } == true

        // If not (completely) cached, remove all files anyways to ensure no broken files are kept
        if (!cached) {
            removeFromCache(file)
        }

        return cached
    }

    fun loadFile(file: FileObject.File): Flow<GcodeFileDataSource.LoadState> = flow {
        try {
            emit(GcodeFileDataSource.LoadState.Loading(0f))
            initJob.join()

            val gcode = try {
                getIndexFile(getCacheKey(file)).inputStream().use {
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
    }.flowOn(Dispatchers.IO)

    @Suppress("BlockingMethodInNonBlockingContext")
    fun loadLayer(cacheKey: CacheKey, layerInfo: LayerInfo) = try {
        val f = RandomAccessFile(getDataFile(cacheKey), "r")
        f.seek(layerInfo.positionInCacheFile)
        val bytes = ByteArray(layerInfo.lengthInCacheFile)
        f.readFully(bytes)
        fstConfig.decodeFromStream(ByteArrayInputStream(bytes)) as Layer
    } catch (e: Exception) {
        removeFromCache(cacheKey)
        Timber.e(e)
        throw e
    }


    fun createCacheForFile(file: FileObject.File) = CacheContext(file, this)

    internal fun removeFromCache(file: FileObject.File) {
        removeFromCache(getCacheKey(file))
    }

    fun removeFromCache(cacheKey: CacheKey) {
        getDataFile(cacheKey).delete()
        getIndexFile(cacheKey).delete()
        sharedPreferences.edit { remove(cacheKey) }
    }

    private fun cleanUp() {
        fun totalSize() = cacheRoot.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
        Timber.i("Cleaning up cache")

        // Delete files until cache size is below max
        while (totalSize() > MAX_CACHE_SIZE) {
            val cacheEntries = sharedPreferences.all.keys.map {
                Pair(it, getCacheEntry(it))
            }.sortedBy {
                it.second.cachedAt
            }.toMutableList()

            if (cacheEntries.size == 1) {
                Timber.i("Only one cache entry left occupying ${totalSize().asStyleFileSize()}")
                break
            }

            if (BuildConfig.DEBUG) {
                Timber.i(
                    "Total size exceeds maximum: %s / %s",
                    totalSize().asStyleFileSize(),
                    MAX_CACHE_SIZE.asStyleFileSize()
                )
            }

            val oldest = cacheEntries.removeAt(0)
            Timber.i("Removing from cache: ${oldest.first}")
            removeFromCache(oldest.first)
        }

        Timber.i("Cache cleaned, occupying ${totalSize().asStyleFileSize()}")
    }

    private fun getCacheEntry(cacheKey: String) =
        gson.fromJson(sharedPreferences.getString(cacheKey, null), CacheEntry::class.java)


    private fun getCacheKey(file: FileObject.File) = "${file.path}:${file.date}:${file.hash}".hashCode().absoluteValue.toString()
    private fun getIndexFile(cacheKey: CacheKey) = File(cacheRoot, "$cacheKey.index")
    private fun getDataFile(cacheKey: CacheKey) = File(cacheRoot, "$cacheKey.bin")

    data class CacheEntry(
        val cachedAt: Long,
        val fileDate: Long,
    )

    data class CacheContext(
        private val file: FileObject.File,
        private val dataSource: LocalGcodeFileDataSource
    ) {
        private var dataBytesWritten = 0L
        private val cacheKey = dataSource.getCacheKey(file)
        private val indexFile = dataSource.getIndexFile(cacheKey)
        private val dataFile = dataSource.getDataFile(cacheKey)

        init {
            dataFile.parentFile?.mkdirs()
            val cacheEntry = CacheEntry(cachedAt = System.currentTimeMillis(), fileDate = file.date)
            dataSource.sharedPreferences.edit {
                putString(cacheKey, dataSource.gson.toJson(cacheEntry))
            }
            Timber.i("Cache context for ${file.path} initialized")
        }

        // After init so parent file is created
        private val dataOutStream = dataFile.outputStream().buffered()

        fun cacheLayer(layer: Layer): Layer {
            val positionInCacheFile = dataBytesWritten
            val bytes = ByteArrayOutputStream()
            dataSource.fstConfig.encodeToStream(bytes, layer)
            dataOutStream.write(bytes.toByteArray())
            dataBytesWritten += bytes.size()

            // Upgrade layer with info where in the cache the file is
            return layer.copy(info = layer.info.copy(positionInCacheFile = positionInCacheFile, lengthInCacheFile = bytes.size()))
        }

        private fun finalize(gcode: Gcode): Gcode {
            dataOutStream.close()

            // Delete old file exists
            val upgradedGcode = gcode.copy(cacheKey = cacheKey)

            indexFile.outputStream().use {
                dataSource.fstConfig.encodeToStream(it, upgradedGcode)
            }

            Timber.i("Added to cache: ${file.path} (cacheKey=$cacheKey)")
            dataSource.cleanUp()
            return upgradedGcode
        }

        private fun abort() {
            dataOutStream.close()
            dataSource.removeFromCache(file)
        }

        suspend fun use(block: suspend (CacheContext) -> Gcode): Gcode {
            try {
                try {
                    dataSource.initJob.join()
                    return finalize(block(this))
                } catch (e: OutOfMemoryError) {
                    throw IOException(e)
                }
            } catch (e: Exception) {
                abort()
                Timber.e(e)
                throw e
            }
        }
    }
}

typealias CacheKey = String

