package de.crysxd.octoapp.base.ui.temperature

import androidx.lifecycle.MutableLiveData
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.usecase.SetBedTargetTemperatureUseCase
import de.crysxd.octoapp.base.usecase.SetToolTargetTemperatureUseCase
import de.crysxd.octoapp.octoprint.models.printer.PrinterState
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ControlBedTemperatureViewModel(
    octoPrintProvider: OctoPrintProvider,
    useCase: SetBedTargetTemperatureUseCase
) : ControlTemperatureViewModelContract(octoPrintProvider, useCase) {

    companion object {
        val sharedManualOverwriteLiveData = MutableLiveData<PrinterState.ComponentTemperature?>()
    }

    override val manualOverwriteLiveData = sharedManualOverwriteLiveData

    override fun extractComponentTemperature(pst: PrinterState.PrinterTemperature) = pst.bed

    override fun getComponentName() = R.string.bed
}