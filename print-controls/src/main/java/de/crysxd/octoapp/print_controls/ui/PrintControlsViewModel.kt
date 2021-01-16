package de.crysxd.octoapp.print_controls.ui

import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.livedata.OctoTransformations.filter
import de.crysxd.octoapp.base.livedata.OctoTransformations.filterEventsForMessageType
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.base.ui.BaseViewModel
import de.crysxd.octoapp.base.usecase.*
import de.crysxd.octoapp.octoprint.models.socket.Message.CurrentMessage
import kotlinx.coroutines.launch

class PrintControlsViewModel(
    octoPrintRepository: OctoPrintRepository,
    octoPrintProvider: OctoPrintProvider,
    private val cancelPrintJobUseCase: CancelPrintJobUseCase,
    private val togglePausePrintJobUseCase: TogglePausePrintJobUseCase,
    private val emergencyStopUseCase: EmergencyStopUseCase,
    private val changeFilamentUseCase: ChangeFilamentUseCase
) : BaseViewModel() {

    val printState = octoPrintProvider.eventLiveData
        .filterEventsForMessageType(CurrentMessage::class.java)
        .filter { it.progress != null }

    val instanceInformation = octoPrintRepository.instanceInformationFlow()
        .asLiveData()

    fun togglePausePrint() = viewModelScope.launch(coroutineExceptionHandler) {
        togglePausePrintJobUseCase.execute()
    }
}