package de.crysxd.octoapp.base.ui.widget.temperature

import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.usecase.SetBedTargetTemperatureUseCase
import de.crysxd.octoapp.octoprint.models.printer.PrinterState
import de.crysxd.octoapp.octoprint.models.socket.HistoricTemperatureData

class ControlBedTemperatureWidgetViewModel(
    octoPrintProvider: OctoPrintProvider,
    useCase: SetBedTargetTemperatureUseCase
) : ControlTemperatureWidgetViewModelContract(octoPrintProvider, useCase) {

    override fun extractComponentTemperature(temp: HistoricTemperatureData): PrinterState.ComponentTemperature = temp.bed

    override fun getComponentName() = R.string.bed
}