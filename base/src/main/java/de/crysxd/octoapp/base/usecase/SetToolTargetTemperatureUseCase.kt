package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.octoprint.models.printer.ToolCommand
import timber.log.Timber
import javax.inject.Inject

class SetToolTargetTemperatureUseCase @Inject constructor(
    private val octoPrintProvider: OctoPrintProvider
) : UseCase<SetToolTargetTemperatureUseCase.Param, Unit>() {

    override suspend fun doExecute(param: Param, timber: Timber.Tree) {
        octoPrintProvider.octoPrint().createPrinterApi().executeToolCommand(
            ToolCommand.SetTargetTemperatureToolCommand(
                ToolCommand.TemperatureSet(param.toolTemperature)
            )
        )
    }

    data class Param(
        val toolTemperature: Int
    )
}