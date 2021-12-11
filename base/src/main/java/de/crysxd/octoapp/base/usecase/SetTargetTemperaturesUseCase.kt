package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.base.network.OctoPrintProvider
import de.crysxd.octoapp.octoprint.models.printer.BedCommand
import de.crysxd.octoapp.octoprint.models.printer.ChamberCommand
import de.crysxd.octoapp.octoprint.models.printer.ToolCommand
import javax.inject.Inject

class SetTargetTemperaturesUseCase @Inject constructor(
    octoPrintProvider: OctoPrintProvider,
    getCurrentPrinterProfileUseCase: GetCurrentPrinterProfileUseCase,
) : BaseChangeTemperaturesUseCase(
    octoPrintProvider = octoPrintProvider,
    getCurrentPrinterProfileUseCase = getCurrentPrinterProfileUseCase
) {
    override fun createBedCommand(temperature: Int) = BedCommand.SetTargetTemperature(temperature)
    override fun createChamberCommand(temperature: Int) = ChamberCommand.SetTargetTemperature(temperature)
    override fun createToolCommand(temperature: ToolCommand.TemperatureSet) = ToolCommand.SetTargetTemperature(temperature)
}
