package de.crysxd.octoapp.help.troubleshoot

import androidx.lifecycle.asLiveData
import de.crysxd.baseui.BaseViewModel
import de.crysxd.octoapp.base.data.models.ResolvedWebcamSettings
import de.crysxd.octoapp.base.data.repository.OctoPrintRepository
import de.crysxd.octoapp.base.network.OctoPrintProvider
import de.crysxd.octoapp.base.usecase.GetWebcamSettingsUseCase
import de.crysxd.octoapp.base.usecase.TestFullNetworkStackUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
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

    private val retrySignalChannel = MutableStateFlow(0)
    val uiState = octoPrintProvider.octoPrintFlow()
        .combine(retrySignalChannel) { _, _ ->
            // No data returned, we only need a trigger :)
        }.flatMapLatest {
            flow {
                emit(UiState.Loading)
                val start = System.currentTimeMillis()
                val instance = octoPrintRepository.getActiveInstanceSnapshot()
                val activeIndex = instance?.appSettings?.activeWebcamIndex ?: 0
                val webcamSettings = getWebcamSettingsUseCase.execute(instance)!![activeIndex]
                val mjpegSettings = webcamSettings as? ResolvedWebcamSettings.MjpegSettings
                    ?: return@flow emit(UiState.UnsupportedWebcam)
                val target = TestFullNetworkStackUseCase.Target.Webcam(mjpegSettings)
                val finding = testFullNetworkStackUseCase.execute(target)
                val end = System.currentTimeMillis()
                val delay = MIN_LOADING_TIME - (end - start)
                if (delay > 0) delay(delay)
                emit(UiState.Finding(finding))
            }
        }.catch {
            Timber.e(it)
            emit(UiState.Finding(TestFullNetworkStackUseCase.Finding.UnexpectedIssue(null, it)))
        }.asLiveData()

    fun retry() = retrySignalChannel.value++

    sealed class UiState {
        object Loading : UiState()
        data class Finding(val finding: TestFullNetworkStackUseCase.Finding) : UiState()
        object UnsupportedWebcam : UiState()
    }
}