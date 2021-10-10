package de.crysxd.octoapp.preprintcontrols.ui

import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import de.crysxd.baseui.BaseViewModel
import de.crysxd.octoapp.base.data.repository.OctoPrintRepository
import de.crysxd.octoapp.base.usecase.ChangeFilamentUseCase
import de.crysxd.octoapp.base.usecase.TurnOffPsuUseCase
import de.crysxd.octoapp.base.usecase.execute
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class PrePrintControlsViewModel(
    octoPrintRepository: OctoPrintRepository,
    private val turnOffPsuUseCase: TurnOffPsuUseCase,
    private val changeFilamentUseCase: ChangeFilamentUseCase
) : BaseViewModel() {

    val webCamSupported = octoPrintRepository.instanceInformationFlow()
        .map { it?.isWebcamSupported != false }
        .distinctUntilChanged()
        .asLiveData()

    fun changeFilament() = viewModelScope.launch(coroutineExceptionHandler) {
        changeFilamentUseCase.execute()
    }
}