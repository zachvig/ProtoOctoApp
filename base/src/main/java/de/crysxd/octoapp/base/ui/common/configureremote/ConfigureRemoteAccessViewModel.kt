package de.crysxd.octoapp.base.ui.common.configureremote

import androidx.lifecycle.*
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.base.ui.base.BaseViewModel
import de.crysxd.octoapp.base.usecase.SetAlternativeWebUrlUseCase
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.lang.Exception

class ConfigureRemoteAccessViewModel(
    octoPrintRepository: OctoPrintRepository,
    private val setAlternativeWebUrlUseCase: SetAlternativeWebUrlUseCase,
) : BaseViewModel() {

    private val mutableViewState = MediatorLiveData<ViewState>()
    val viewState = mutableViewState.map { it }

    private val mutableViewEvents = MutableLiveData<ViewEvent>()
    val viewEvents = mutableViewEvents.map { it }

    init {
        mutableViewState.addSource(
            octoPrintRepository.instanceInformationFlow().map {
                ViewState.Updated(it?.alternativeWebUrl ?: "")
            }.asLiveData()
        ) {
            mutableViewState.postValue(it)
        }
    }

    fun setRemoteUrl(url: String, bypassChecks: Boolean) {
        viewModelScope.launch(coroutineExceptionHandler) {
            mutableViewState.postValue(ViewState.Loading)
            val event = when (val result = setAlternativeWebUrlUseCase.execute(SetAlternativeWebUrlUseCase.Params(url, bypassChecks))) {
                SetAlternativeWebUrlUseCase.Result.Success -> ViewEvent.Success
                is SetAlternativeWebUrlUseCase.Result.Failure -> ViewEvent.ShowError(
                    message = result.errorMessage,
                    exception = result.exception,
                    ignoreAction = if (result.allowToProceed) {
                        { setRemoteUrl(url, true) }
                    } else {
                        null
                    }
                )
            }
            mutableViewEvents.postValue(event)
            mutableViewState.postValue(ViewState.Idle)
        }
    }

    sealed class ViewEvent {
        var consumed = false

        data class ShowError(
            val message: String,
            val exception: Exception,
            val ignoreAction: (() -> Unit)?
        ) : ViewEvent()

        object Success : ViewEvent()
    }

    sealed class ViewState {
        object Idle : ViewState()
        data class Updated(val remoteWebUrl: String) : ViewState()
        object Loading : ViewState()
    }
}