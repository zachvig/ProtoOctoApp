package de.crysxd.octoapp.signin.probe

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import de.crysxd.octoapp.base.ui.base.BaseViewModel
import de.crysxd.octoapp.base.usecase.ProbeOctoPrintUseCase
import kotlinx.coroutines.launch

class ProbeOctoPrintViewModel(
    private val useCase: ProbeOctoPrintUseCase
) : BaseViewModel() {

    private val mutableUiState = MutableLiveData<UiState>(UiState.Loading)
    val uiState = mutableUiState.map { it }

    fun probe(webUrl: String) = viewModelScope.launch(coroutineExceptionHandler) {
        mutableUiState.postValue(UiState.Loading)
        val findings = useCase.execute(ProbeOctoPrintUseCase.Params(webUrl))
        mutableUiState.postValue(UiState.FindingsReady(findings))

    }

    sealed class UiState {
        object Loading : UiState()
        data class FindingsReady(val findings: List<ProbeOctoPrintUseCase.Finding>) : UiState()
    }
}