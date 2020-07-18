package de.crysxd.octoapp.base.usecase

import android.os.Bundle
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import de.crysxd.octoapp.octoprint.OctoPrint
import de.crysxd.octoapp.octoprint.models.files.FileCommand
import de.crysxd.octoapp.octoprint.models.files.FileObject
import javax.inject.Inject

class StartPrintJobUseCase @Inject constructor() : UseCase<Pair<OctoPrint, FileObject.File>, Unit> {

    override suspend fun execute(param: Pair<OctoPrint, FileObject.File>) {
        Firebase.analytics.logEvent("print_started_by_app", Bundle.EMPTY)
        param.first.createFilesApi().executeFileCommand(param.second, FileCommand.SelectFile(true))
    }
}