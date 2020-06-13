package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.octoprint.OctoPrint
import de.crysxd.octoapp.octoprint.models.job.JobCommand
import de.crysxd.octoapp.octoprint.models.printer.GcodeCommand
import javax.inject.Inject

class EmergencyStopUseCase @Inject constructor() : UseCase<OctoPrint, Unit> {

    override suspend fun execute(param: OctoPrint) {
        param.createPrinterApi().executeGcodeCommand(GcodeCommand.Single("M112"))
    }
}