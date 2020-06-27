package de.crysxd.octoapp.base.ui.widget.temperature

import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.usecase.SetToolTargetTemperatureUseCase
import de.crysxd.octoapp.octoprint.models.printer.PrinterState
import de.crysxd.octoapp.octoprint.models.socket.HistoricTemperatureData

class ControlToolTemperatureWidgetViewModel(
    octoPrintProvider: OctoPrintProvider,
    useCase: SetToolTargetTemperatureUseCase
) : ControlTemperatureWidgetViewModelContract(octoPrintProvider, useCase) {

    override fun extractComponentTemperature(temp: HistoricTemperatureData): PrinterState.ComponentTemperature = temp.tool0

    override fun getComponentName() = R.string.hotend
}