package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.octoprint.models.job.JobCommand
import timber.log.Timber
import javax.inject.Inject

class CancelPrintJobUseCase @Inject constructor(
    private val octoPrintProvider: OctoPrintProvider
) : UseCase<Unit, Unit>() {

    override suspend fun doExecute(param: Unit, timber: Timber.Tree) {
        OctoAnalytics.logEvent(OctoAnalytics.Event.PrintCancelledByApp)
        octoPrintProvider.octoPrint().createJobApi().executeJobCommand(JobCommand.CancelJobCommand)
    }
}