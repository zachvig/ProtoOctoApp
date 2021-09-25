package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.base.network.OctoPrintProvider
import de.crysxd.octoapp.octoprint.models.files.FileObject
import de.crysxd.octoapp.octoprint.models.files.FileOrigin
import timber.log.Timber
import javax.inject.Inject

class LoadFilesUseCase @Inject constructor(
    private val octoPrintProvider: OctoPrintProvider
) : UseCase<LoadFilesUseCase.Params, List<FileObject>>() {

    override suspend fun doExecute(param: Params, timber: Timber.Tree): List<FileObject> {
        return octoPrintProvider.octoPrint().createFilesApi().getFiles(param.fileOrigin, param.folder).files
    }

    data class Params(
        val fileOrigin: FileOrigin,
        val folder: FileObject.Folder?
    )
}
