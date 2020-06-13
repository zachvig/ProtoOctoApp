package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.octoprint.OctoPrint
import de.crysxd.octoapp.octoprint.models.job.JobCommand

class PausePrintUseCase : UseCase<OctoPrint, Unit> {

    override suspend fun execute(param: OctoPrint): Unit {
        param.createJobApi().executeJobCommand(JobCommand.PauseJobCommand)
    }
}