package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.base.ext.awaitFileChange
import de.crysxd.octoapp.base.network.OctoPrintProvider
import de.crysxd.octoapp.octoprint.models.files.FileObject
import timber.log.Timber
import javax.inject.Inject

class DeleteFileUseCase @Inject constructor(
    private val octoPrintProvider: OctoPrintProvider
) : UseCase<FileObject, Unit>() {

    override suspend fun doExecute(param: FileObject, timber: Timber.Tree) {
        octoPrintProvider.octoPrint().createFilesApi().deleteFile(param)

        // Await changes to take affect
        octoPrintProvider.awaitFileChange()
    }
}