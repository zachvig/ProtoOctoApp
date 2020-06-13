package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.octoprint.OctoPrint
import de.crysxd.octoapp.octoprint.models.files.FileObject
import de.crysxd.octoapp.octoprint.models.files.FileOrigin
import javax.inject.Inject

class LoadFilesUseCase @Inject constructor() : UseCase<Triple<OctoPrint, FileOrigin, FileObject.Folder?>, List<FileObject>> {

    override suspend fun execute(param: Triple<OctoPrint, FileOrigin, FileObject.Folder?>): List<FileObject> {
        return param.first.createFilesApi().getFiles(param.second).files
    }
}
