package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.octoprint.OctoPrint
import de.crysxd.octoapp.octoprint.models.files.FileCommand
import de.crysxd.octoapp.octoprint.models.files.FileObject
import javax.inject.Inject

class StartPrintJobUseCase @Inject constructor() : UseCase<Pair<OctoPrint, FileObject.File>, Unit> {

    override suspend fun execute(param: Pair<OctoPrint, FileObject.File>) {
        param.first.createFilesApi().executeFileCommand(param.second, FileCommand.SelectFile(true))
    }
}