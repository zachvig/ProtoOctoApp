package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.base.network.OctoPrintProvider
import de.crysxd.octoapp.octoprint.models.printer.BedCommand
import de.crysxd.octoapp.octoprint.models.printer.ChamberCommand
import de.crysxd.octoapp.octoprint.models.printer.ToolCommand
import javax.inject.Inject

class SetTemperatureOffsetUseCase @Inject constructor(
    octoPrintProvider: OctoPrintProvider,
    getCurrentPrinterProfileUseCase: GetCurrentPrinterProfileUseCase,
) : BaseChangeTemperaturesUseCase(
    octoPrintProvider = octoPrintProvider,
    getCurrentPrinterProfileUseCase = getCurrentPrinterProfileUseCase
) {
    override fun createBedCommand(temperature: Int) = BedCommand.SetTemperatureOffset(temperature)
    override fun createChamberCommand(temperature: Int) = ChamberCommand.SetTemperatureOffset(temperature)
    override fun createToolCommand(temperature: ToolCommand.TemperatureSet) = ToolCommand.SetTemperatureOffset(temperature)
}
