package de.crysxd.octoapp.base.datasource

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import de.crysxd.octoapp.base.gcode.parse.models.Gcode
import de.crysxd.octoapp.octoprint.models.files.FileObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.*

private const val MAX_CACHE_SIZE = 16 * 1024 * 1024 // 16 MB

class LocalGcodeFileDataSource(
    context: Context,
    private val gson: Gson,
    private val sharedPreferences: SharedPreferences
) : GcodeFileDataSource {

    private val cacheRoot = File(context.cacheDir, "gcode")

    init {
        cacheRoot.mkdirs()
    }

    override fun canLoadFile(file: FileObject.File): Boolean = sharedPreferences.contains(file.cacheKey)

    @Suppress("BlockingMethodInNonBlockingContext")
    override fun loadFile(file: FileObject.File): Flow<GcodeFileDataSource.LoadState> = flow<GcodeFileDataSource.LoadState> {
        withContext(Dispatchers.IO) {
            try {
                emit(GcodeFileDataSource.LoadState.Loading)
                val cacheEntry = gson.fromJson(sharedPreferences.getString(file.cacheKey, null), CacheEntry::class.java)

                val gcode = cacheEntry.localFile.inputStream().use {
                    ObjectInputStream(it).readObject() as Gcode
                }

                emit(GcodeFileDataSource.LoadState.Ready(gcode))
            } catch (e: Exception) {
                sharedPreferences.edit().remove(file.cacheKey)
                Timber.e(e)
                emit(GcodeFileDataSource.LoadState.Failed(e))
            }
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun cacheGcode(file: FileObject.File, gcode: Gcode) = withContext(Dispatchers.IO) {
        val cacheEntry = CacheEntry(
            updatedAt = Date(),
            localFile = createCacheFile()
        )

        cacheEntry.localFile.outputStream().use {
            ObjectOutputStream(it).writeObject(gcode)
        }

        sharedPreferences.edit {
            putString(file.cacheKey, gson.toJson(cacheEntry))
        }

        cleanUp()
    }

    private suspend fun cleanUp() = withContext(Dispatchers.IO) {
        fun totalSize() = cacheRoot.listFiles()?.sumByDouble { it.length().toDouble() }?.toLong() ?: 0L
        val cacheEntries = sharedPreferences.all.map {
            Pair(it.key, gson.fromJson(it.value as String, CacheEntry::class.java))
        }.sortedBy {
            it.second.updatedAt
        }.toMutableList()

        // Delete files until cache size is below max
        while (totalSize() > MAX_CACHE_SIZE) {
            val oldest = cacheEntries.removeAt(0)
            sharedPreferences.edit { remove(oldest.first) }
            oldest.second.localFile.delete()

        }
    }

    private val FileObject.File.cacheKey get() = path

    private fun createCacheFile() = File(cacheRoot, UUID.randomUUID().toString())

    data class CacheEntry(
        val updatedAt: Date,
        val localFile: File,
    )
}