package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.octoprint.OctoPrint
import de.crysxd.octoapp.octoprint.models.printer.ToolCommand
import javax.inject.Inject

class ExtrudeFilamentUseCase @Inject constructor() : UseCase<Pair<OctoPrint, Int>, Unit> {

    override suspend fun execute(param: Pair<OctoPrint, Int>) {
        param.first.createPrinterApi().executeToolCommand(ToolCommand.ExtrudeFilamentToolCommand(param.second))
    }
}