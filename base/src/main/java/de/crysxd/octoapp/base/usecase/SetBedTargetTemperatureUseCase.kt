package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.octoprint.OctoPrint
import de.crysxd.octoapp.octoprint.models.printer.BedCommand
import timber.log.Timber
import javax.inject.Inject

class SetBedTargetTemperatureUseCase @Inject constructor() : UseCase<Pair<OctoPrint, Int>, Unit>() {

    override suspend fun doExecute(param: Pair<OctoPrint, Int>, timber: Timber.Tree) {
        param.first.createPrinterApi().executeBedCommand(
            BedCommand.SetTargetTemperatureToolCommand(param.second)
        )
    }
}