package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.base.ext.awaitFileChange
import de.crysxd.octoapp.base.network.OctoPrintProvider
import de.crysxd.octoapp.octoprint.models.files.FileObject
import de.crysxd.octoapp.octoprint.models.files.FileOrigin
import timber.log.Timber
import javax.inject.Inject

class CreateFolderUseCase @Inject constructor(private val octoPrintProvider: OctoPrintProvider) : UseCase<CreateFolderUseCase.Params, Unit>() {

    override suspend fun doExecute(param: Params, timber: Timber.Tree) {
        // Trigger creation, the server will complete the action async
        octoPrintProvider.octoPrint().createFilesApi().createFolder(
            origin = param.origin,
            parent = param.parent,
            name = param.name,
        )

        // Await changes
        octoPrintProvider.awaitFileChange()
    }

    data class Params(
        val origin: FileOrigin,
        val parent: FileObject.Folder?,
        val name: String,
    )
}