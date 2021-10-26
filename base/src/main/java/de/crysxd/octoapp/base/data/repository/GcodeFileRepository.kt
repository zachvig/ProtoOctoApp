package de.crysxd.octoapp.base.data.repository

import de.crysxd.octoapp.base.data.source.LocalGcodeFileDataSource
import de.crysxd.octoapp.base.data.source.RemoteGcodeFileDataSource
import de.crysxd.octoapp.octoprint.models.files.FileObject
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retry
import timber.log.Timber

@Suppress("EXPERIMENTAL_API_USAGE")
class GcodeFileRepository(
    private val localDataSource: LocalGcodeFileDataSource,
    private val remoteDataSource: RemoteGcodeFileDataSource,
) {
    fun loadFile(file: FileObject.File, allowLargeFileDownloads: Boolean) = flow {
        if (!localDataSource.canLoadFile(file)) {
            Timber.i("Loading ${file.path} from remote")
            emit(remoteDataSource.loadFile(file, allowLargeFileDownloads))
        } else {
            Timber.i("Loading ${file.path} from local")
            emit(localDataSource.loadFile(file))
        }
    }.flatMapLatest {
        it
    }.retry(1) {
        Timber.w(it, "Failed to load ${file.path}, removing from cache and trying again")
        localDataSource.removeFromCache(file)
        true
    }.catch {
        Timber.e(it, "Failed to load ${file.path} after clearing cache")
        localDataSource.removeFromCache(file)
    }
}