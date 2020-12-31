package de.crysxd.octoapp.base.datasource

import de.crysxd.octoapp.base.gcode.parse.models.Gcode
import de.crysxd.octoapp.octoprint.models.files.FileObject
import kotlinx.coroutines.flow.Flow

interface GcodeFileDataSource {

    fun canLoadFile(file: FileObject.File): Boolean

    fun loadFile(file: FileObject.File, allowLargeFileDownloads: Boolean): Flow<LoadState>

    suspend fun cacheGcode(file: FileObject.File, gcode: Gcode)

    sealed class LoadState {
        data class Loading(val progress: Float) : LoadState()
        object FailedLargeFileDownloadRequired : LoadState()
        data class Ready(val gcode: Gcode) : LoadState()
        data class Failed(val exception: Throwable) : LoadState()
    }
}