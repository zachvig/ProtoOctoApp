package de.crysxd.octoapp.print_controls.ui

import androidx.lifecycle.viewModelScope
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.livedata.OctoTransformations.filter
import de.crysxd.octoapp.base.livedata.OctoTransformations.filterEventsForMessageType
import de.crysxd.octoapp.base.ui.BaseViewModel
import de.crysxd.octoapp.base.usecase.CancelPrintJobUseCase
import de.crysxd.octoapp.base.usecase.EmergencyStopUseCase
import de.crysxd.octoapp.base.usecase.TogglePausePrintJobUseCase
import de.crysxd.octoapp.octoprint.models.socket.Message
import kotlinx.coroutines.launch

class PrintControlsViewModel(
    private val octoPrintProvider: OctoPrintProvider,
    private val cancelPrintJobUseCase: CancelPrintJobUseCase,
    private val togglePausePrintJobUseCase: TogglePausePrintJobUseCase,
    private val emergencyStopUseCase: EmergencyStopUseCase
) : BaseViewModel() {

    val printState = octoPrintProvider.eventLiveData
        .filterEventsForMessageType(Message.CurrentMessage::class.java)
        .filter { it.progress != null }

    fun togglePausePrint() = viewModelScope.launch(coroutineExceptionHandler) {
        octoPrintProvider.octoPrint.value?.let {
            togglePausePrintJobUseCase.execute(it)
        }
    }

    fun cancelPrint() = viewModelScope.launch(coroutineExceptionHandler) {
        octoPrintProvider.octoPrint.value?.let {
            cancelPrintJobUseCase.execute(it)
        }
    }

    fun emergencyStop() = viewModelScope.launch(coroutineExceptionHandler) {
        octoPrintProvider.octoPrint.value?.let {
            emergencyStopUseCase.execute(it)
        }
    }

    fun getOctoPrintUrl() = octoPrintProvider.octoPrint.value?.gerWebUrl()

}