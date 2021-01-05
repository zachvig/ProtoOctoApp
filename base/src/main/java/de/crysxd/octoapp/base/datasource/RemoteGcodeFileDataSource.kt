package de.crysxd.octoapp.base.datasource

import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.gcode.parse.GcodeParser
import de.crysxd.octoapp.base.gcode.parse.models.Gcode
import de.crysxd.octoapp.base.utils.measureTime
import de.crysxd.octoapp.octoprint.models.files.FileObject
import de.crysxd.octoapp.octoprint.models.settings.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import timber.log.Timber

class RemoteGcodeFileDataSource(
    private val gcodeParser: GcodeParser,
    private val octoPrintProvider: OctoPrintProvider
) : GcodeFileDataSource {

    override fun canLoadFile(file: FileObject.File) = true

    override fun loadFile(file: FileObject.File, allowLargeFileDownloads: Boolean) = flow {
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
                    gcodeParser.parseFile(it, file.size) { progress ->
                        emit(GcodeFileDataSource.LoadState.Loading(progress))
                    }
                }
            } catch (e: Exception) {
                Timber.e(e)
                null
            }
        } ?: return@flow emit(GcodeFileDataSource.LoadState.Failed(IllegalStateException("Unable to download or parse file")))

        emit(GcodeFileDataSource.LoadState.Ready(gcode))
    }.flowOn(Dispatchers.IO)

    override suspend fun cacheGcode(file: FileObject.File, gcode: Gcode) = Unit
}