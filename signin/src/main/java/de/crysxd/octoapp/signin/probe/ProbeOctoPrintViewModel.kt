package de.crysxd.octoapp.signin.probe

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import de.crysxd.octoapp.base.ui.base.BaseViewModel
import de.crysxd.octoapp.base.usecase.TestFullNetworkStackUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ProbeOctoPrintViewModel(
    private val useCase: TestFullNetworkStackUseCase
) : BaseViewModel() {

    companion object {
        private const val MIN_PROBE_DURATION = 2000
    }

    var webUrl: String = ""
    private val mutableUiState = MutableLiveData<UiState>(UiState.Loading)
    val uiState = mutableUiState.map { it }

    fun probe() = viewModelScope.launch(coroutineExceptionHandler) {
        val start = System.currentTimeMillis()
        mutableUiState.postValue(UiState.Loading)
        val finding = useCase.execute(TestFullNetworkStackUseCase.Params(webUrl))
        (MIN_PROBE_DURATION - (System.currentTimeMillis() - start)).takeIf { it > 0 }?.let { delay(it) }
        mutableUiState.postValue(UiState.FindingsReady(finding))
    }

    sealed class UiState {
        object Loading : UiState()
        data class FindingsReady(val finding: TestFullNetworkStackUseCase.Finding?) : UiState()
    }
}