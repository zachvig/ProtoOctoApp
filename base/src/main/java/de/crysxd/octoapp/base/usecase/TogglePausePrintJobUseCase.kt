package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.octoprint.OctoPrint
import de.crysxd.octoapp.octoprint.models.job.JobCommand
import timber.log.Timber
import javax.inject.Inject

class TogglePausePrintJobUseCase @Inject constructor() : UseCase<OctoPrint, Unit>() {

    override suspend fun doExecute(param: OctoPrint, timber: Timber.Tree) {
        param.createJobApi().executeJobCommand(JobCommand.TogglePauseCommand)
    }
}