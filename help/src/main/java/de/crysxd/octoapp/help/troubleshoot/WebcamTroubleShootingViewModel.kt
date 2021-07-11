package de.crysxd.octoapp.help.troubleshoot

import androidx.lifecycle.asLiveData
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.base.ui.base.BaseViewModel
import de.crysxd.octoapp.base.usecase.GetWebcamSettingsUseCase
import de.crysxd.octoapp.base.usecase.TestFullNetworkStackUseCase
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import timber.log.Timber

@Suppress("EXPERIMENTAL_API_USAGE")
class WebcamTroubleShootingViewModel(
    octoPrintRepository: OctoPrintRepository,
    octoPrintProvider: OctoPrintProvider,
    getWebcamSettingsUseCase: GetWebcamSettingsUseCase,
    testFullNetworkStackUseCase: TestFullNetworkStackUseCase,
) : BaseViewModel() {

    companion object {
        private const val MIN_LOADING_TIME = 2000
    }

    private val retrySignalChannel = ConflatedBroadcastChannel(Unit)
    val uiState = octoPrintProvider.octoPrintFlow()
        .combine(retrySignalChannel.asFlow()) { _, _ ->
            // No data returned, we only need a trigger :)
        }.flatMapLatest {
            flow {
                emit(UiState.Loading)
                val start = System.currentTimeMillis()
                val instance = octoPrintRepository.getActiveInstanceSnapshot()
                val activeIndex = instance?.appSettings?.activeWebcamIndex ?: 0
                val webcamSettings = getWebcamSettingsUseCase.execute(instance)!![activeIndex]
                val target = TestFullNetworkStackUseCase.Target.Webcam(webcamSettings)
                val finding = testFullNetworkStackUseCase.execute(target)
                val end = System.currentTimeMillis()
                val delay = MIN_LOADING_TIME - (end - start)
                if (delay > 0) delay(delay)
                emit(UiState.Finding(finding))
            }
        }.catch {
            Timber.e(it)
            emit(UiState.Finding(TestFullNetworkStackUseCase.Finding.UnexpectedIssue("", it)))
        }.asLiveData()

    fun retry() = retrySignalChannel.offer(Unit)

    sealed class UiState {
        object Loading : UiState()
        data class Finding(val finding: TestFullNetworkStackUseCase.Finding) : UiState()
    }
}