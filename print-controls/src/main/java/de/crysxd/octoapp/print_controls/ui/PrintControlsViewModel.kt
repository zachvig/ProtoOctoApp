package de.crysxd.octoapp.print_controls.ui

import androidx.lifecycle.viewModelScope
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.livedata.OctoTransformations.filter
import de.crysxd.octoapp.base.livedata.OctoTransformations.filterEventsForMessageType
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.base.ui.BaseViewModel
import de.crysxd.octoapp.base.usecase.CancelPrintJobUseCase
import de.crysxd.octoapp.base.usecase.ChangeFilamentUseCase
import de.crysxd.octoapp.base.usecase.EmergencyStopUseCase
import de.crysxd.octoapp.base.usecase.TogglePausePrintJobUseCase
import de.crysxd.octoapp.octoprint.models.socket.Message
import kotlinx.coroutines.launch

class PrintControlsViewModel(
    octoPrintRepository: OctoPrintRepository,
    private val octoPrintProvider: OctoPrintProvider,
    private val cancelPrintJobUseCase: CancelPrintJobUseCase,
    private val togglePausePrintJobUseCase: TogglePausePrintJobUseCase,
    private val emergencyStopUseCase: EmergencyStopUseCase,
    private val changeFilamentUseCase: ChangeFilamentUseCase
) : BaseViewModel() {

    val printState = octoPrintProvider.eventLiveData
        .filterEventsForMessageType(Message.CurrentMessage::class.java)
        .filter { it.progress != null }

    val instanceInformation = octoPrintRepository.instanceInformation

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

    fun changeFilament() = viewModelScope.launch(coroutineExceptionHandler) {
        octoPrintProvider.octoPrint.value?.let {
            changeFilamentUseCase.execute(it)
        }
    }

    fun emergencyStop() = viewModelScope.launch(coroutineExceptionHandler) {
        octoPrintProvider.octoPrint.value?.let {
            emergencyStopUseCase.execute(it)
        }
    }
}