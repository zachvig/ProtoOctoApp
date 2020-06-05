package de.crysxd.octoapp.base.ui.temperature

import androidx.lifecycle.MutableLiveData
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.usecase.SetToolTargetTemperatureUseCase
import de.crysxd.octoapp.octoprint.models.printer.PrinterState
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ControlToolTemperatureViewModel(
    private val octoPrintProvider: OctoPrintProvider,
    private val useCase: SetToolTargetTemperatureUseCase
) : ControlTemperatureViewModelContract(octoPrintProvider) {

    companion object {
        val sharedManualOverwriteLiveData = MutableLiveData<PrinterState.ComponentTemperature?>()
    }

    override val manualOverwriteLiveData = sharedManualOverwriteLiveData

    override fun extractComponentTemperature(pst: PrinterState.PrinterTemperature) = pst.tool0

    override fun applyTemperature(temp: Int) {
        GlobalScope.launch(coroutineExceptionHandler) {
            octoPrintProvider.octoPrint.value?.let {
                useCase.execute(Pair(it, temp))
            }
        }
    }

    override fun getComponentName() = R.string.hotend
}