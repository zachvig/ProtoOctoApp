package de.crysxd.octoapp.base.datasource

import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.gcode.parse.GcodeParser
import de.crysxd.octoapp.base.utils.measureTime
import de.crysxd.octoapp.octoprint.models.files.FileObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class RemoteGcodeFileDataSource(
    private val gcodeParses: List<GcodeParser>,
    private val localDataSource: LocalGcodeFileDataSource,
    private val octoPrintProvider: OctoPrintProvider
) : GcodeFileDataSource {

    override fun canLoadFile(file: FileObject.File) = true

    override fun loadFile(file: FileObject.File) = flow {
        emit(GcodeFileDataSource.LoadState.Loading)

        Timber.i("Downloading file...")
        val fileContent = measureTime("Download file") {
            withContext(Dispatchers.IO) {
                octoPrintProvider.octoPrint().createFilesApi().downloadFile(file)?.reader()?.readText()
            }
        } ?: return@flow emit(GcodeFileDataSource.LoadState.Failed(IllegalStateException("Unable to download file")))

        val gcode = measureTime("Parse file") {
            withContext(Dispatchers.Default) {
                try {
                    val parser = measureTime("  Find parser") {
                        gcodeParses.firstOrNull { it.canParseFile(fileContent) }
                    }
                    measureTime("  Parsing gcode") {
                        parser?.parseFile(fileContent)
                    }
                } catch (e: Exception) {
                    Timber.e(e)
                    null
                }
            }
        } ?: return@flow emit(GcodeFileDataSource.LoadState.Failed(IllegalStateException("Unable to parse file")))

        // Let's not let the user wait for us creating the cache entry
        GlobalScope.launch {
            try {
                measureTime("Cache file") {
                    localDataSource.cacheGcode(file, gcode)
                }
            } catch (e: Exception) {
                Timber.e(e)
            }
        }

        emit(GcodeFileDataSource.LoadState.Ready(gcode))
    }
}