package de.crysxd.octoapp.base.data.repository

import android.content.res.Resources
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import android.util.TypedValue
import androidx.core.net.toUri
import de.crysxd.octoapp.base.data.source.LocalMediaFileDataSource
import de.crysxd.octoapp.base.data.source.RemoteMediaFileDataSource
import de.crysxd.octoapp.base.network.OctoPrintProvider
import de.crysxd.octoapp.base.utils.AppScope
import de.crysxd.octoapp.octoprint.models.socket.Event
import de.crysxd.octoapp.octoprint.models.socket.Message.EventMessage.MovieDone
import de.crysxd.octoapp.octoprint.models.socket.Message.EventMessage.MovieFailed
import de.crysxd.octoapp.octoprint.models.socket.Message.EventMessage.MovieRendering
import de.crysxd.octoapp.octoprint.models.timelapse.TimelapseConfig
import de.crysxd.octoapp.octoprint.models.timelapse.TimelapseFile
import de.crysxd.octoapp.octoprint.models.timelapse.TimelapseStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File

class TimelapseRepository(
    private val octoPrintProvider: OctoPrintProvider,
    private val localMediaFileDataSource: LocalMediaFileDataSource,
    private val remoteMediaFileDataSource: RemoteMediaFileDataSource,
) {

    // The int is used to push forced updates after a thumbnail was fetched
    private val state = MutableStateFlow<Pair<TimelapseStatus, Int>?>(null)

    init {
        AppScope.launch {
            octoPrintProvider.passiveEventFlow().retry { delay(1000); true }.collect {
                if (it is Event.MessageReceived && (it.message is MovieFailed || it.message is MovieDone || it.message is MovieRendering)) {
                    fetchLatest()
                }
            }
        }
    }

    suspend fun update(block: TimelapseConfig.() -> TimelapseConfig) {
        val current = state.value
        requireNotNull(current) { "Timelapse state not loaded" }
        requireNotNull(current.first.config) { "Timelapse config not loaded" }
        val new = block(current.first.config!!)
        Timber.d("Updating timelapse config $current -> $new")
        state.value = octoPrintProvider.octoPrint().createTimelapseApi().updateConfig(new) to (state.value?.second ?: 0)
    }

    suspend fun delete(timelapseFile: TimelapseFile) {
        state.value = (octoPrintProvider.octoPrint().createTimelapseApi().delete(timelapseFile) ?: fetchLatest()) to (state.value?.second ?: 0)
    }

    suspend fun fetchLatest(): TimelapseStatus {
        val latest = octoPrintProvider.octoPrint().createTimelapseApi().getStatus()
        state.value = latest to (state.value?.second ?: 0)
        Timber.d("Got latest timelapse config $latest")
        return latest
    }

    suspend fun download(timelapseFile: TimelapseFile) = withContext(Dispatchers.IO) {
        val url = remoteMediaFileDataSource.ensureAbsoluteUrl(timelapseFile.url)
        return@withContext localMediaFileDataSource.load(url) ?: let {
            Timber.i("Downloading: ${timelapseFile.url}")
            val file = localMediaFileDataSource.store(url, remoteMediaFileDataSource.loadFromOctoPrint(url))

            try {
                Timber.i("Generating thumbnail for: ${timelapseFile.url}")
                requireNotNull(file) { "Just downloaded file is null" }
                createThumbnail(file)?.let {
                    val bytes = ByteArrayOutputStream()
                    it.compress(Bitmap.CompressFormat.JPEG, 80, bytes)
                    localMediaFileDataSource.store("thumb:$url", bytes.toByteArray().inputStream())
                }

                // Update flow so thumbnail is refreshed in UI
                state.value = state.value?.let { it.first to it.second + 1 }
            } catch (e: Exception) {
                Timber.e(e)
            }

            file
        }
    }

    suspend fun getThumbnailUri(timelapseFile: TimelapseFile) = withContext(Dispatchers.IO) {
        try {
            val url = remoteMediaFileDataSource.ensureAbsoluteUrl(timelapseFile.url)
            return@withContext localMediaFileDataSource.load("thumb:$url")?.toUri()
        } catch (e: Exception) {
            Timber.e(e)
            null
        }
    }

    private fun createThumbnail(file: File) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ThumbnailUtils.createVideoThumbnail(file, Size(200f.toPx(), 200f.toPx()), null)
    } else {
        @Suppress("Deprecation")
        ThumbnailUtils.createVideoThumbnail(file.path, MediaStore.Images.Thumbnails.MINI_KIND)
    }

    private fun Float.toPx() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this, Resources.getSystem().displayMetrics).toInt()

    fun peek() = state.value

    fun flow() = state.asStateFlow().map { it?.first }
}