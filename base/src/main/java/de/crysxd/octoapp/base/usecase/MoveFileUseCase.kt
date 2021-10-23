package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.base.ext.awaitFileChange
import de.crysxd.octoapp.base.network.OctoPrintProvider
import de.crysxd.octoapp.octoprint.models.files.FileCommand
import de.crysxd.octoapp.octoprint.models.files.FileObject
import timber.log.Timber
import javax.inject.Inject

class MoveFileUseCase @Inject constructor(
    private val octoPrintProvider: OctoPrintProvider
) : UseCase<MoveFileUseCase.Params, Unit>() {

    override suspend fun doExecute(param: Params, timber: Timber.Tree) {
        octoPrintProvider.octoPrint().createFilesApi().executeFileCommand(
            file = param.file,
            command = FileCommand.MoveFile(
                destination = param.newPath
            )
        )

        // Await changes to take affect
        octoPrintProvider.awaitFileChange()
    }

    data class Params(
        val file: FileObject,
        val newPath: String,
    )
}