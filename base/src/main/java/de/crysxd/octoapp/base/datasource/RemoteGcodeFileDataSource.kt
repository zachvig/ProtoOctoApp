package de.crysxd.octoapp.base.datasource

import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.gcode.parse.GcodeParser
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
) {

    fun loadFile(file: FileObject.File, allowLargeFileDownloads: Boolean) = flow {
        val maxFileSize = octoPrintProvider.octoPrint().createSettingsApi()
            .getSettings().plugins.values.mapNotNull { it as? Settings.GcodeViewerSettings }
            .firstOrNull()?.mobileSizeThreshold

        if (maxFileSize != null && !allowLargeFileDownloads && file.size > maxFileSize) {
            return@flow emit(GcodeFileDataSource.LoadState.FailedLargeFileDownloadRequired)
        }

        emit(GcodeFileDataSource.LoadState.Loading(0f))

        val gcode = measureTime("Download and parse file") {
            try {
                octoPrintProvider.octoPrint().createFilesApi().downloadFile(file)?.use {
                    val gcode = GcodeParser(
                        content = it,
                        totalSize = file.size,
                        progressUpdate = { progress ->
                            emit(GcodeFileDataSource.LoadState.Loading(progress))
                        },
                        layerSink = { layer ->
                            localDataSource.cacheGcodeLayer(file, layer)
                        }
                    ).parseFile()
                    localDataSource.cacheGcode(file, gcode)
                }
            } catch (e: Exception) {
                Timber.e(e)
                localDataSource.removeFromCache(file)
                null
            }
        } ?: return@flow emit(GcodeFileDataSource.LoadState.Failed(IllegalStateException("Unable to download or parse file")))

        emit(GcodeFileDataSource.LoadState.Ready(gcode))
    }.flowOn(Dispatchers.IO)
}