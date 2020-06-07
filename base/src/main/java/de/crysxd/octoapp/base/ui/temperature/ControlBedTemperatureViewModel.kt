package de.crysxd.octoapp.base.ui.temperature

import androidx.lifecycle.MutableLiveData
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.usecase.SetBedTargetTemperatureUseCase
import de.crysxd.octoapp.base.usecase.SetToolTargetTemperatureUseCase
import de.crysxd.octoapp.octoprint.models.printer.PrinterState
import de.crysxd.octoapp.octoprint.models.socket.HistoricTemperatureData
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ControlBedTemperatureViewModel(
    octoPrintProvider: OctoPrintProvider,
    useCase: SetBedTargetTemperatureUseCase
) : ControlTemperatureViewModelContract(octoPrintProvider, useCase) {

    override fun extractComponentTemperature(temp: HistoricTemperatureData): PrinterState.ComponentTemperature = temp.bed

    override fun getComponentName() = R.string.bed
}