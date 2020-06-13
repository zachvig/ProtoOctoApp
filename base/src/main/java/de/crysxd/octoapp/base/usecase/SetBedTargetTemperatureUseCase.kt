package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.octoprint.OctoPrint
import de.crysxd.octoapp.octoprint.models.printer.BedCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SetBedTargetTemperatureUseCase @Inject constructor() : UseCase<Pair<OctoPrint, Int>, Unit> {

    override suspend fun execute(param: Pair<OctoPrint, Int>) {
        param.first.createPrinterApi().executeBedCommand(
            BedCommand.SetTargetTemperatureToolCommand(param.second)
        )
    }
}