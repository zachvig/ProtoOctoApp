package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.octoprint.OctoPrint
import de.crysxd.octoapp.octoprint.models.printer.ToolCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SetToolTargetTemperatureUseCase : UseCase<Pair<OctoPrint, Int>, Unit> {

    override suspend fun execute(param: Pair<OctoPrint, Int>) {
        param.first.createPrinterApi().executeToolCommand(
            ToolCommand.SetTargetTemperatureToolCommand(
                ToolCommand.TemperatureSet(param.second)
            )
        )
    }
}