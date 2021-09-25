package de.crysxd.octoapp.base.data.source

import de.crysxd.octoapp.base.gcode.parse.models.Gcode
import de.crysxd.octoapp.octoprint.models.files.FileObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber

class MemoryGcodeFileDataSource : GcodeFileDataSource {

    private var cache = mutableMapOf<Int, Gcode>()

    override fun canLoadFile(file: FileObject.File) = cache.containsKey(file.cacheId)

    override fun loadFile(file: FileObject.File, allowLargeFileDownloads: Boolean): Flow<GcodeFileDataSource.LoadState> = flow {
        cache[file.cacheId]?.also {
            Timber.i("Restore from memory: ${file.path}")
            emit(GcodeFileDataSource.LoadState.Ready(it))
        } ?: emit(GcodeFileDataSource.LoadState.Failed(IllegalStateException("Requested item from memory cache which is not cached")))
    }

    override suspend fun cacheGcode(file: FileObject.File, gcode: Gcode) {
        // We only cache one
        Timber.i("Cache in memory: ${file.path}")
        cache.clear()
        cache[file.cacheId] = gcode
    }

    val FileObject.File.cacheId get() = "$path$date".hashCode()
}