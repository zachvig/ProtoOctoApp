package de.crysxd.octoapp.pre_print_controls.ui

import androidx.lifecycle.viewModelScope
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.PollingLiveData
import de.crysxd.octoapp.base.ui.BaseViewModel
import de.crysxd.octoapp.base.usecase.SetToolTargetTemperatureUseCase
import de.crysxd.octoapp.octoprint.models.printer.ToolCommand
import kotlinx.coroutines.launch

class PrePrintControlsViewModel(
    private val octoPrintProvider: OctoPrintProvider,
    private val setToolTargetTemperatureUseCase: SetToolTargetTemperatureUseCase
) : BaseViewModel() {

    val printerState = octoPrintProvider.printerState

    fun setTool0Temperature(temp: Int) =
        viewModelScope.launch(coroutineExceptionHandler) {
            if (temp != (printerState.value as? PollingLiveData.Result.Success)?.result?.temperature?.tool0?.target?.toInt()) {
                octoPrintProvider.octoPrint.value?.let {
                    setToolTargetTemperatureUseCase.execute(Pair(it, temp))
                }
            }
        }

    private fun setBedTemperature(temp: Float) {

    }
}