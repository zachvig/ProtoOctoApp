package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.octoprint.OctoPrint
import de.crysxd.octoapp.octoprint.models.files.FileObject
import de.crysxd.octoapp.octoprint.models.files.FileOrigin
import javax.inject.Inject

class LoadFilesUseCase @Inject constructor() : UseCase<LoadFilesUseCase.Params, List<FileObject>> {

    override suspend fun execute(param: Params): List<FileObject> {
        return param.octoPrint.createFilesApi().getFiles(param.fileOrigin, param.folder).files
    }

    data class Params(
        val octoPrint: OctoPrint,
        val fileOrigin: FileOrigin,
        val folder: FileObject.Folder?
    )
}
