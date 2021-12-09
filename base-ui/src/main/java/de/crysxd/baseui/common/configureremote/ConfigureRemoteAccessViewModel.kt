package de.crysxd.baseui.common.configureremote

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import de.crysxd.baseui.BaseViewModel
import de.crysxd.octoapp.base.data.models.OctoEverywhereConnection
import de.crysxd.octoapp.base.data.repository.OctoPrintRepository
import de.crysxd.octoapp.base.usecase.GetRemoteServiceConnectUrlUseCase
import de.crysxd.octoapp.base.usecase.SetAlternativeWebUrlUseCase
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import okhttp3.HttpUrl

class ConfigureRemoteAccessViewModel(
    octoPrintRepository: OctoPrintRepository,
    private val setAlternativeWebUrlUseCase: SetAlternativeWebUrlUseCase,
    private val getRemoteServiceConnectUrlUseCase: GetRemoteServiceConnectUrlUseCase,
) : BaseViewModel() {

    private val mutableViewState = MutableLiveData<ViewState>()
    val viewState = mutableViewState.map { it }

    val viewData: LiveData<ViewData>

    private val mutableViewEvents = MutableLiveData<ViewEvent>()
    val viewEvents = mutableViewEvents.map { it }

    init {
        var lastRemoteUrl: HttpUrl? = null
        var first = true
        viewData = octoPrintRepository.instanceInformationFlow().map {
            // We did not have a octoeverywhere connection before but now we have one -> Freshly connected. Show success.
            if (it?.alternativeWebUrl != lastRemoteUrl && !first) {
                mutableViewEvents.postValue(ViewEvent.Success())
            }
            first = false
            lastRemoteUrl = it?.alternativeWebUrl

            ViewData(
                remoteWebUrl = it?.alternativeWebUrl,
                octoEverywhereConnection = it?.octoEverywhereConnection
            )
        }.asLiveData()
    }

    fun getOctoEverywhereAppPortalUrl() = getRemoteServiceSetupUrl(GetRemoteServiceConnectUrlUseCase.RemoteService.OctoEverywhere)

    fun getSpaghettiDetectiveSetupUrl() = getRemoteServiceSetupUrl(GetRemoteServiceConnectUrlUseCase.RemoteService.SpaghettiDetective)

    private fun getRemoteServiceSetupUrl(service: GetRemoteServiceConnectUrlUseCase.RemoteService) = viewModelScope.launch(coroutineExceptionHandler) {
        mutableViewState.postValue(ViewState.Loading)
        val event = when (val result = getRemoteServiceConnectUrlUseCase.execute(service)) {
            is GetRemoteServiceConnectUrlUseCase.Result.Success -> ViewEvent.OpenUrl(result.url)
            is GetRemoteServiceConnectUrlUseCase.Result.Error -> ViewEvent.ShowError(
                message = result.errorMessage,
                exception = result.exception,
                ignoreAction = null
            )
        }
        mutableViewEvents.postValue(event)
        mutableViewState.postValue(ViewState.Idle)
    }

    fun setRemoteUrl(url: String, username: String, password: String, bypassChecks: Boolean) {
        viewModelScope.launch(coroutineExceptionHandler) {
            mutableViewState.postValue(ViewState.Loading)
            val params = SetAlternativeWebUrlUseCase.Params(
                webUrl = url,
                password = password,
                username = username,
                bypassChecks = bypassChecks
            )
            val event = when (val result = setAlternativeWebUrlUseCase.execute(params)) {
                SetAlternativeWebUrlUseCase.Result.Success -> ViewEvent.Success()
                is SetAlternativeWebUrlUseCase.Result.Failure -> ViewEvent.ShowError(
                    message = result.errorMessage,
                    exception = result.exception,
                    ignoreAction = if (result.allowToProceed) {
                        { setRemoteUrl(url = url, username = username, password = password, bypassChecks = true) }
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

    data class ViewData(val remoteWebUrl: HttpUrl?, val octoEverywhereConnection: OctoEverywhereConnection?)

}