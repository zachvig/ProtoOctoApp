package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.octoprint.OctoPrint
import de.crysxd.octoapp.octoprint.models.job.JobCommand
import javax.inject.Inject

class CancelPrintJobUseCase @Inject constructor() : UseCase<OctoPrint, Unit> {

    override suspend fun execute(param: OctoPrint) {
        param.createJobApi().executeJobCommand(JobCommand.CancelJobCommand)
    }
}