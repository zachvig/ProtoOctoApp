package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.base.network.OctoPrintProvider
import de.crysxd.octoapp.octoprint.models.job.JobCommand
import timber.log.Timber
import javax.inject.Inject

class TogglePausePrintJobUseCase @Inject constructor(
    private val octoPrintProvider: OctoPrintProvider
) : UseCase<Unit, Unit>() {

    override suspend fun doExecute(param: Unit, timber: Timber.Tree) {
        octoPrintProvider.octoPrint().createJobApi().executeJobCommand(JobCommand.TogglePauseCommand)
    }
}