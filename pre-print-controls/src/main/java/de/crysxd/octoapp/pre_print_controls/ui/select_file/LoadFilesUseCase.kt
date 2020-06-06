package de.crysxd.octoapp.pre_print_controls.ui.select_file

import de.crysxd.octoapp.base.usecase.UseCase
import de.crysxd.octoapp.octoprint.OctoPrint
import de.crysxd.octoapp.octoprint.models.files.FileObject
import de.crysxd.octoapp.octoprint.models.files.FileOrigin

class LoadFilesUseCase : UseCase<Triple<OctoPrint, FileOrigin, FileObject.Folder?>, List<FileObject>> {

    override suspend fun execute(param: Triple<OctoPrint, FileOrigin, FileObject.Folder?>): List<FileObject> {
        return param.first.createFilesApi().getFiles(param.second).files
    }
}
