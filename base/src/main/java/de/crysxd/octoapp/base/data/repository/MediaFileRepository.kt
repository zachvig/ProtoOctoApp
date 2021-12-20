package de.crysxd.octoapp.base.data.repository

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import de.crysxd.octoapp.base.data.source.LocalMediaFileDataSource
import de.crysxd.octoapp.base.data.source.RemoteMediaFileDataSource
import de.crysxd.octoapp.base.utils.AppScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class MediaFileRepository(
    private val localDataSource: LocalMediaFileDataSource,
    private val remoteDataSource: RemoteMediaFileDataSource,
) {

    // We load on AppScope because we don't want to "waste" a started download.
    fun getMediaUri(url: String, lifecycleOwner: LifecycleOwner, then: (Uri) -> Unit) = AppScope.launch(Dispatchers.IO) {
        try {
            val file = localDataSource.load(url) ?: let {
                // Store as non-deletable. We don't want to ever load this media again
                localDataSource.store(url, remoteDataSource.load(url), canBeDeleted = false)
            } ?: throw IllegalStateException("Not cached after loading")

            // Check if caller is still alive
            withContext(Dispatchers.Main) {
                if (lifecycleOwner.lifecycle.currentState >= Lifecycle.State.INITIALIZED) {
                    val uri = file.toUri()
                    Timber.i("Continuing with $uri")
                    then(uri)
                } else {
                    Timber.w("Lifecycle died")
                }
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }
}