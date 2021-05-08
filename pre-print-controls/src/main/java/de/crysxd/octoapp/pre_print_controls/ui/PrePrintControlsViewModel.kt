package de.crysxd.octoapp.pre_print_controls.ui

import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import de.crysxd.octoapp.base.UriLibrary
import de.crysxd.octoapp.base.ext.open
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.base.ui.base.BaseViewModel
import de.crysxd.octoapp.base.usecase.ChangeFilamentUseCase
import de.crysxd.octoapp.base.usecase.TurnOffPsuUseCase
import de.crysxd.octoapp.base.usecase.execute
import de.crysxd.octoapp.pre_print_controls.R
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class PrePrintControlsViewModel(
    octoPrintRepository: OctoPrintRepository,
    private val turnOffPsuUseCase: TurnOffPsuUseCase,
    private val changeFilamentUseCase: ChangeFilamentUseCase
) : BaseViewModel() {

    val webCamSupported = octoPrintRepository.instanceInformationFlow()
        .map { it?.isWebcamSupported == true }
        .distinctUntilChanged()
        .asLiveData()

    fun changeFilament() = viewModelScope.launch(coroutineExceptionHandler) {
        changeFilamentUseCase.execute()
    }
}