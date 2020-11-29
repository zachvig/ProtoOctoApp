package de.crysxd.octoapp.base.datasource

import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.gcode.parse.GcodeParser
import de.crysxd.octoapp.octoprint.models.files.FileObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
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

        val fileContent = withContext(Dispatchers.IO) {
            octoPrintProvider.octoPrint().createFilesApi().downloadFile(file)?.reader()?.readText()
        } ?: return@flow emit(GcodeFileDataSource.LoadState.Failed(IllegalStateException("Unable to download file")))


        val gcode = withContext(Dispatchers.Default) {
            gcodeParses.firstOrNull { it.canParseFile(fileContent) }?.parseFile(fileContent)
        } ?: return@flow emit(GcodeFileDataSource.LoadState.Failed(IllegalStateException("Unable to parse file")))

        try {
            localDataSource.cacheGcode(file, gcode)
        } catch (e: Exception) {
            Timber.e(e)
        }

        emit(GcodeFileDataSource.LoadState.Ready(gcode))
    }
}