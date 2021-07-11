package de.crysxd.octoapp.signin.apikey

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import de.crysxd.octoapp.base.ui.base.BaseViewModel
import de.crysxd.octoapp.base.usecase.TestFullNetworkStackUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ManualApiKeyViewModel(
    private val testFullNetworkStackUseCase: TestFullNetworkStackUseCase,
) : BaseViewModel() {

    companion object {
        private const val MIN_LOADING_DELAY = 1000
    }

    private val mutableUiState = MutableLiveData<ViewState>(ViewState.Idle)
    val uiState = mutableUiState.map { it }

    fun testApiKey(webUrl: String, apiKey: String) = viewModelScope.launch(coroutineExceptionHandler) {
        mutableUiState.postValue(ViewState.Loading)
        val start = System.currentTimeMillis()
        val newState = when (testFullNetworkStackUseCase.execute(TestFullNetworkStackUseCase.Target.OctoPrint(webUrl = webUrl, apiKey = apiKey))) {
            is TestFullNetworkStackUseCase.Finding.OctoPrintReady -> ViewState.Success(webUrl = webUrl, apiKey = apiKey)
            is TestFullNetworkStackUseCase.Finding.InvalidApiKey -> ViewState.InvalidApiKey()
            else -> ViewState.UnexpectedError()
        }
        val end = System.currentTimeMillis()
        val delay = MIN_LOADING_DELAY - (end - start)
        if (delay > 0) delay(delay)
        mutableUiState.postValue(newState)
    }

    sealed class ViewState {
        object Idle : ViewState()
        object Loading : ViewState()
        data class Success(val webUrl: String, val apiKey: String) : ViewState()
        data class InvalidApiKey(var handled: Boolean = false) : ViewState()
        data class UnexpectedError(var handled: Boolean = false) : ViewState()
    }
}