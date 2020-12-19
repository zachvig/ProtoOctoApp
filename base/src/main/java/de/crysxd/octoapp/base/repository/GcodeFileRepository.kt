package de.crysxd.octoapp.base.repository

import de.crysxd.octoapp.base.datasource.GcodeFileDataSource
import de.crysxd.octoapp.base.datasource.GcodeFileDataSourceGroup
import de.crysxd.octoapp.octoprint.models.files.FileObject
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber

class GcodeFileRepository(
    private val dataSources: GcodeFileDataSourceGroup
) {
    fun loadFile(file: FileObject.File, allowLargeFileDownloads: Boolean): Flow<GcodeFileDataSource.LoadState> {
        Timber.i("Loading ${file.path}")
        return (dataSources.dataSources.firstOrNull {
            it.canLoadFile(file)
        }?.loadFile(file, allowLargeFileDownloads) ?: flow {
            emit(GcodeFileDataSource.LoadState.Failed(IllegalStateException("No data source can load file")))
        }).onEach { state ->
            if (state is GcodeFileDataSource.LoadState.Ready) {
                dataSources.dataSources.forEach {
                    GlobalScope.launch {
                        try {
                            it.cacheGcode(file, state.gcode)
                        } catch (e: Exception) {
                            Timber.i(e)
                        }
                    }
                }
            }
        }
    }
}