package de.crysxd.octoapp.base.data.repository

import de.crysxd.octoapp.base.data.source.GcodeFileDataSource
import de.crysxd.octoapp.base.data.source.LocalGcodeFileDataSource
import de.crysxd.octoapp.base.data.source.RemoteGcodeFileDataSource
import de.crysxd.octoapp.octoprint.models.files.FileObject
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

class GcodeFileRepository(
    private val localDataSource: LocalGcodeFileDataSource,
    private val remoteDataSource: RemoteGcodeFileDataSource,
) {
    fun loadFile(file: FileObject.File, allowLargeFileDownloads: Boolean): Flow<GcodeFileDataSource.LoadState> {
        return if (!localDataSource.canLoadFile(file)) {
            Timber.i("Loading ${file.path} from remote")
            remoteDataSource.loadFile(file, allowLargeFileDownloads)
        } else {
            Timber.i("Loading ${file.path} from local")
            localDataSource.loadFile(file)
        }
    }
}