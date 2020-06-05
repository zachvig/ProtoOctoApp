package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.octoprint.OctoPrint
import de.crysxd.octoapp.octoprint.models.printer.GcodeCommand

class ExecuteGcodeCommandUseCase : UseCase<Pair<OctoPrint, GcodeCommand>, Unit> {

    override suspend fun execute(param: Pair<OctoPrint, GcodeCommand>) {
        param.first.createPrinterApi().executeGcodeCommand(param.second)
    }
}