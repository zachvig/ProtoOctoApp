package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.base.network.OctoPrintProvider
import de.crysxd.octoapp.octoprint.models.files.FileObject
import de.crysxd.octoapp.octoprint.models.files.FileOrigin
import timber.log.Timber
import javax.inject.Inject

class LoadFileUseCase @Inject constructor(
    private val octoPrintProvider: OctoPrintProvider
) : UseCase<LoadFileUseCase.Params, FileObject.File>() {

    override suspend fun doExecute(param: Params, timber: Timber.Tree): FileObject.File {
        return octoPrintProvider.octoPrint().createFilesApi().getFile(param.fileOrigin, param.path)
    }

    data class Params(
        val fileOrigin: FileOrigin,
        val path: String
    )
}
