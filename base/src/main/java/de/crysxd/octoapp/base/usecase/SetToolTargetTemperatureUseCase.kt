package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.octoprint.OctoPrint
import de.crysxd.octoapp.octoprint.models.printer.ToolCommand
import timber.log.Timber
import javax.inject.Inject

class SetToolTargetTemperatureUseCase @Inject constructor() : UseCase<Pair<OctoPrint, Int>, Unit>() {

    override suspend fun doExecute(param: Pair<OctoPrint, Int>, timber: Timber.Tree) {
        param.first.createPrinterApi().executeToolCommand(
            ToolCommand.SetTargetTemperatureToolCommand(
                ToolCommand.TemperatureSet(param.second)
            )
        )
    }
}