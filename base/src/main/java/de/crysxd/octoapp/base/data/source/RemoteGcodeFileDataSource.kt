package de.crysxd.octoapp.base.data.source

import android.content.Context
import android.net.ConnectivityManager
import de.crysxd.octoapp.base.ext.asStyleFileSize
import de.crysxd.octoapp.base.gcode.parse.GcodeParser
import de.crysxd.octoapp.base.network.OctoPrintProvider
import de.crysxd.octoapp.base.utils.measureTime
import de.crysxd.octoapp.octoprint.models.files.FileObject
import de.crysxd.octoapp.octoprint.models.settings.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import timber.log.Timber

class RemoteGcodeFileDataSource(
    private val octoPrintProvider: OctoPrintProvider,
    private val localDataSource: LocalGcodeFileDataSource,
    private val context: Context,
) {

    fun loadFile(file: FileObject.File, allowLargeFileDownloads: Boolean) = flow {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val onMeteredNetwork = connectivityManager.isActiveNetworkMetered

        val maxFileSize = octoPrintProvider.octoPrint().createSettingsApi()
            .getSettings().plugins.values.mapNotNull { it as? Settings.GcodeViewerSettings }
            .firstOrNull()?.let {
                if (onMeteredNetwork) it.mobileSizeThreshold else it.sizeThreshold
            }.also {
                Timber.i("Current network is metered: $onMeteredNetwork -> max file size for direct download is ${it?.asStyleFileSize()}")
            }

        if (maxFileSize != null && !allowLargeFileDownloads && (file.size ?: Long.MAX_VALUE) > maxFileSize) {
            return@flow emit(GcodeFileDataSource.LoadState.FailedLargeFileDownloadRequired)
        }

        emit(GcodeFileDataSource.LoadState.Loading(0f))

        measureTime("Download and parse file") {
            try {
                val gcode = octoPrintProvider.octoPrint().createFilesApi().downloadFile(file)?.use { input ->
                    localDataSource.createCacheForFile(file).use { cache ->
                        GcodeParser(
                            content = input,
                            totalSize = file.size ?: Long.MAX_VALUE,
                            progressUpdate = { progress ->
                                emit(GcodeFileDataSource.LoadState.Loading(progress))
                            },
                            layerSink = { cache.cacheLayer(it) }
                        ).parseFile()
                    }
                } ?: throw java.lang.IllegalStateException("Unable to download or parse file")
                emit(GcodeFileDataSource.LoadState.Ready(gcode))
            } catch (e: Exception) {
                Timber.e(e)
                emit(GcodeFileDataSource.LoadState.Failed(e))
            }
        }
    }.flowOn(Dispatchers.IO)
}