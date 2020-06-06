package de.crysxd.octoapp.pre_print_controls.ui.select_file

import de.crysxd.octoapp.base.usecase.UseCase
import de.crysxd.octoapp.octoprint.OctoPrint
import de.crysxd.octoapp.octoprint.models.files.FileList

class LoadFilesUseCase : UseCase<OctoPrint, FileList> {

    override suspend fun execute(param: OctoPrint): FileList {
        return param.createFilesApi().getFiles()
    }

}
