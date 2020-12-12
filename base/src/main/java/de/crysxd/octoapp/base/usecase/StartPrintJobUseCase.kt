package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.octoprint.models.files.FileCommand
import de.crysxd.octoapp.octoprint.models.files.FileObject
import timber.log.Timber
import javax.inject.Inject

class StartPrintJobUseCase @Inject constructor(
    private val octoPrintProvider: OctoPrintProvider
) : UseCase<FileObject.File, Unit>() {

    override suspend fun doExecute(param: FileObject.File, timber: Timber.Tree) {
        OctoAnalytics.logEvent(OctoAnalytics.Event.PrintStartedByApp)
        octoPrintProvider.octoPrint().createFilesApi().executeFileCommand(param, FileCommand.SelectFile(true))
    }
}