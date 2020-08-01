package de.crysxd.octoapp.pre_print_controls.ui

import androidx.lifecycle.viewModelScope
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.base.ui.BaseViewModel
import de.crysxd.octoapp.base.usecase.ChangeFilamentUseCase
import de.crysxd.octoapp.base.usecase.TurnOffPsuUseCase
import de.crysxd.octoapp.pre_print_controls.R
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class PrePrintControlsViewModel(
    octoPrintRepository: OctoPrintRepository,
    private val octoPrintProvider: OctoPrintProvider,
    private val turnOffPsuUseCase: TurnOffPsuUseCase,
    private val changeFilamentUseCase: ChangeFilamentUseCase
) : BaseViewModel() {

    val instanceInformation = octoPrintRepository.instanceInformation

    fun turnOffPsu() = GlobalScope.launch(coroutineExceptionHandler) {
        octoPrintProvider.octoPrint.value?.let {
            turnOffPsuUseCase.execute(it)
        }
    }

    fun startPrint() {
        navContoller.navigate(R.id.action_start_print)
    }

    fun changeFilament() = viewModelScope.launch(coroutineExceptionHandler) {
        octoPrintProvider.octoPrint.value?.let {
            changeFilamentUseCase.execute(it)
        }
    }
}