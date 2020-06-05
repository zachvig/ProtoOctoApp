package de.crysxd.octoapp.connect_printer.ui

import androidx.lifecycle.viewModelScope
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.ui.BaseViewModel
import de.crysxd.octoapp.base.usecase.TurnOnPsuUseCase
import kotlinx.coroutines.launch

class ConnectPrinterViewModel(private val octoPrintProvider: OctoPrintProvider, private val turnOnPsuUseCase: TurnOnPsuUseCase) : BaseViewModel() {

    val printerState = octoPrintProvider.printerState

    fun turnOnPsu() = viewModelScope.launch(coroutineExceptionHandler) {
        octoPrintProvider.octoPrint.value?.let {
            turnOnPsuUseCase.execute(it)
        }
    }
}