package de.crysxd.octoapp.base.usecase

import android.os.Bundle
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.octoprint.models.files.FileCommand
import de.crysxd.octoapp.octoprint.models.files.FileObject
import timber.log.Timber
import javax.inject.Inject

class StartPrintJobUseCase @Inject constructor(
    private val octoPrintProvider: OctoPrintProvider
) : UseCase<FileObject.File, Unit>() {

    override suspend fun doExecute(param: FileObject.File, timber: Timber.Tree) {
        Firebase.analytics.logEvent("print_started_by_app", Bundle.EMPTY)
        octoPrintProvider.octoPrint().createFilesApi().executeFileCommand(param, FileCommand.SelectFile(true))
    }
}