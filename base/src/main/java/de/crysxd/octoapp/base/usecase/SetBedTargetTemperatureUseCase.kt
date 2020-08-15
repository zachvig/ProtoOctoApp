package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.octoprint.models.printer.BedCommand
import timber.log.Timber
import javax.inject.Inject

class SetBedTargetTemperatureUseCase @Inject constructor(
    private val octoPrintProvider: OctoPrintProvider
) : UseCase<SetBedTargetTemperatureUseCase.Param, Unit>() {

    override suspend fun doExecute(param: Param, timber: Timber.Tree) {
        octoPrintProvider.octoPrint().createPrinterApi().executeBedCommand(
            BedCommand.SetTargetTemperatureToolCommand(param.bedTemperature)
        )
    }

    data class Param(
       val bedTemperature: Int
    )
}