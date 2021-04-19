package de.crysxd.octoapp.print_controls.ui

import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.ext.filterEventsForMessageType
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.base.ui.base.BaseViewModel
import de.crysxd.octoapp.base.usecase.*
import de.crysxd.octoapp.octoprint.models.socket.Message.CurrentMessage
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class PrintControlsViewModel(
    octoPrintRepository: OctoPrintRepository,
    octoPrintProvider: OctoPrintProvider,
    private val togglePausePrintJobUseCase: TogglePausePrintJobUseCase,
) : BaseViewModel() {

    val printState = octoPrintProvider.eventFlow("printcontrols")
        .filterEventsForMessageType<CurrentMessage>()
        .filter { it.progress != null }
        .asLiveData()

    val webCamSupported = octoPrintRepository.instanceInformationFlow()
        .map { it?.isWebcamSupported == true }
        .distinctUntilChanged()
        .asLiveData()

    fun togglePausePrint() = viewModelScope.launch(coroutineExceptionHandler) {
        togglePausePrintJobUseCase.execute()
    }
}