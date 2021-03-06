package de.crysxd.octoapp.base.ui.common.configureremote

import androidx.lifecycle.*
import de.crysxd.octoapp.base.models.OctoEverywhereConnection
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.base.ui.base.BaseViewModel
import de.crysxd.octoapp.base.usecase.GetConnectOctoEverywhereUrlUseCase
import de.crysxd.octoapp.base.usecase.SetAlternativeWebUrlUseCase
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch

class ConfigureRemoteAccessViewModel(
    octoPrintRepository: OctoPrintRepository,
    private val setAlternativeWebUrlUseCase: SetAlternativeWebUrlUseCase,
    private val getConnectOctoEverywhereUrlUseCase: GetConnectOctoEverywhereUrlUseCase,
) : BaseViewModel() {

    private val mutableViewState = MutableLiveData<ViewState>()
    val viewState = mutableViewState.map { it }

    val viewData: LiveData<ViewData>

    private val mutableViewEvents = MutableLiveData<ViewEvent>()
    val viewEvents = mutableViewEvents.map { it }

    init {
        var lastOctoEverywhereConnection: OctoEverywhereConnection? = null
        var first = true
        viewData = octoPrintRepository.instanceInformationFlow().map {
            // We did not have a octoeverywhere connection before but now we have one -> Freshly connected. Show success.
            if (it?.octoEverywhereConnection != lastOctoEverywhereConnection && !first) {
                mutableViewEvents.postValue(ViewEvent.Success())
            }
            first = false
            lastOctoEverywhereConnection = it?.octoEverywhereConnection

            ViewData(
                remoteWebUrl = it?.alternativeWebUrl ?: "",
                octoEverywhereConnection = it?.octoEverywhereConnection
            )
        }.asLiveData()
    }

    fun getOctoEverywhereAppPortalUrl() = viewModelScope.launch(coroutineExceptionHandler) {
        mutableViewState.postValue(ViewState.Loading)
        val event = when (val result = getConnectOctoEverywhereUrlUseCase.execute(Unit)) {
            is GetConnectOctoEverywhereUrlUseCase.Result.Success -> ViewEvent.OpenUrl(result.url)
            is GetConnectOctoEverywhereUrlUseCase.Result.Error -> ViewEvent.ShowError(
                message = result.errorMessage,
                exception = result.exception,
                ignoreAction = null
            )
        }
        mutableViewEvents.postValue(event)
        mutableViewState.postValue(ViewState.Idle)
    }

    fun setRemoteUrl(url: String, bypassChecks: Boolean) {
        viewModelScope.launch(coroutineExceptionHandler) {
            mutableViewState.postValue(ViewState.Loading)
            val event = when (val result = setAlternativeWebUrlUseCase.execute(SetAlternativeWebUrlUseCase.Params(url, bypassChecks))) {
                SetAlternativeWebUrlUseCase.Result.Success -> ViewEvent.Success()
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

        class Success : ViewEvent()

        data class OpenUrl(val url: String) : ViewEvent()
    }

    sealed class ViewState {
        object Idle : ViewState()
        object Loading : ViewState()
    }

    data class ViewData(val remoteWebUrl: String, val octoEverywhereConnection: OctoEverywhereConnection?)

}