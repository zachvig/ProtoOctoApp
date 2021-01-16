package de.crysxd.octoapp.pre_print_controls.ui

import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.base.ui.BaseViewModel
import de.crysxd.octoapp.base.usecase.ChangeFilamentUseCase
import de.crysxd.octoapp.base.usecase.TurnOffPsuUseCase
import de.crysxd.octoapp.base.usecase.execute
import de.crysxd.octoapp.pre_print_controls.R
import kotlinx.coroutines.launch

class PrePrintControlsViewModel(
    octoPrintRepository: OctoPrintRepository,
    private val turnOffPsuUseCase: TurnOffPsuUseCase,
    private val changeFilamentUseCase: ChangeFilamentUseCase
) : BaseViewModel() {

    val instanceInformation = octoPrintRepository.instanceInformationFlow().asLiveData()

    fun startPrint() {
        navContoller.navigate(R.id.action_start_print)
    }

    fun changeFilament() = viewModelScope.launch(coroutineExceptionHandler) {
        changeFilamentUseCase.execute()
    }
}