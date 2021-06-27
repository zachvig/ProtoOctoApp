package de.crysxd.octoapp.signin.access

import androidx.lifecycle.asLiveData
import de.crysxd.octoapp.base.ui.base.BaseViewModel
import de.crysxd.octoapp.base.usecase.RequestApiAccessUseCase
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

@Suppress("EXPERIMENTAL_API_USAGE")
class RequestAccessViewModel(
    private val requestApiAccessUseCase: RequestApiAccessUseCase
) : BaseViewModel() {

    private val webUrlChannel = ConflatedBroadcastChannel<String>()
    val uiState = webUrlChannel.asFlow().flatMapLatest {
        requestApiAccessUseCase.execute(RequestApiAccessUseCase.Params(webUrl = it))
    }.map {
        when (it) {
            is RequestApiAccessUseCase.State.AccessGranted -> UiState.AccessGranted(apiKey = it.apiKey)
            RequestApiAccessUseCase.State.Failed -> UiState.ManualApiKeyRequired
            RequestApiAccessUseCase.State.Pending -> UiState.PendingApproval
        }
    }.asLiveData()

    fun useWebUrl(webUrl: String) = if (webUrlChannel.valueOrNull == null) {
        webUrlChannel.offer(webUrl)
    } else {
        false
    }

    sealed class UiState {
        object ManualApiKeyRequired : UiState()
        object PendingApproval : UiState()
        data class AccessGranted(val apiKey: String) : UiState()
    }
}