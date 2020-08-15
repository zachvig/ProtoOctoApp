package de.crysxd.octoapp.base.ui.widget.temperature

import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.usecase.SetBedTargetTemperatureUseCase
import de.crysxd.octoapp.octoprint.models.printer.PrinterState
import de.crysxd.octoapp.octoprint.models.socket.HistoricTemperatureData

class ControlBedTemperatureWidgetViewModel(
    octoPrintProvider: OctoPrintProvider,
    private val useCase: SetBedTargetTemperatureUseCase
) : ControlTemperatureWidgetViewModelContract(octoPrintProvider) {

    override suspend fun setTemperature(temp: Int) {
        useCase.execute(SetBedTargetTemperatureUseCase.Param(temp))
    }

    override fun extractComponentTemperature(temp: HistoricTemperatureData): PrinterState.ComponentTemperature = temp.bed

    override fun getComponentName() = R.string.bed
}