package de.crysxd.octoapp.base.ui.widget.webcam

import android.graphics.Bitmap
import android.net.Uri
import android.widget.ImageView
import androidx.lifecycle.*
import de.crysxd.octoapp.base.billing.BillingManager
import de.crysxd.octoapp.base.ext.isHlsStreamUrl
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.base.ui.BaseViewModel
import de.crysxd.octoapp.base.usecase.ApplyWebcamTransformationsUseCase
import de.crysxd.octoapp.base.usecase.GetWebcamSettingsUseCase
import de.crysxd.octoapp.octoprint.models.settings.WebcamSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

@Suppress("EXPERIMENTAL_API_USAGE")
class WebcamViewModel(
    private val octoPrintRepository: OctoPrintRepository,
    private val getWebcamSettingsUseCase: GetWebcamSettingsUseCase,
    private val applyWebcamTransformationsUseCase: ApplyWebcamTransformationsUseCase,
) : BaseViewModel() {

    private var previousSource: LiveData<UiState>? = null
    private val uiStateMediator = MediatorLiveData<UiState>()
    val uiState = uiStateMediator.map { it }
    private val octoPrintLiveData = octoPrintRepository.instanceInformationFlow()
        .distinctUntilChangedBy { it?.settings?.webcam }
        .asLiveData()

    init {
        uiStateMediator.addSource(octoPrintLiveData) { connect() }
        uiStateMediator.postValue(UiState.Loading)
    }

    fun connect() {
        previousSource?.let(uiStateMediator::removeSource)

        val liveData = BillingManager.billingFlow().map {
            flow {
                try {
                    emit(UiState.Loading)

                    // Load settings
                    val webcamSettings = getWebcamSettingsUseCase.execute(null)
                    val streamUrl = webcamSettings.streamUrl

                    // Check if webcam is configured
                    if (!webcamSettings.webcamEnabled || streamUrl.isNullOrBlank()) {
                        return@flow emit(UiState.WebcamNotConfigured)
                    }

                    // Open stream
                    if (streamUrl.isHlsStreamUrl) {
                        if (!BillingManager.isFeatureEnabled("hls_webcam")) {
                            emit(UiState.HlsStreamDisabled)
                        } else {
                            emit(UiState.HlsStreamReady(Uri.parse(streamUrl), webcamSettings.streamRatio))
                        }
                    } else {
                        MjpegConnection(streamUrl)
                            .load()
                            .map {
                                when (it) {
                                    is MjpegConnection.MjpegSnapshot.Loading -> UiState.Loading
                                    is MjpegConnection.MjpegSnapshot.Frame -> UiState.FrameReady(
                                        frame = applyTransformations(it.frame, webcamSettings),
                                        aspectRation = webcamSettings.streamRatio
                                    )
                                }
                            }
                            .flowOn(Dispatchers.Default)
                            .catch {
                                Timber.e(it)
                                emit(
                                    UiState.Error(
                                        isManualReconnect = true,
                                        streamUrl = webcamSettings.streamUrl
                                    )
                                )
                            }
                            .collect {
                                emit(it)
                            }
                    }
                } catch (e: Exception) {
                    Timber.e(e)
                    emit(UiState.Error(true))
                }
            }
        }.flatMapLatest {
            it
        }.asLiveData()

        previousSource = liveData
        uiStateMediator.addSource(liveData) { uiStateMediator.postValue(it) }
    }

    fun storeScaleType(scaleType: ImageView.ScaleType, isFullscreen: Boolean) = viewModelScope.launch(coroutineExceptionHandler) {
        octoPrintRepository.updateAppSettingsForActive {
            if (isFullscreen) {
                it.copy(webcamFullscreenScaleType = scaleType.ordinal)
            } else {
                it.copy(webcamScaleType = scaleType.ordinal)
            }
        }
    }

    fun getScaleType(isFullscreen: Boolean, default: ImageView.ScaleType) = ImageView.ScaleType.values()[
            octoPrintRepository.getActiveInstanceSnapshot()?.appSettings?.let {
                if (isFullscreen) it.webcamFullscreenScaleType else it.webcamScaleType
            } ?: default.ordinal
    ]

    fun getInitialAspectRatio() = octoPrintRepository.getActiveInstanceSnapshot()?.settings?.webcam?.streamRatio ?: "16:9"

    private suspend fun applyTransformations(frame: Bitmap, webcamSettings: WebcamSettings) =
        applyWebcamTransformationsUseCase.execute(ApplyWebcamTransformationsUseCase.Params(frame, webcamSettings))

    sealed class UiState {
        object Loading : UiState()
        object WebcamNotConfigured : UiState()
        object HlsStreamDisabled : UiState()
        data class FrameReady(val frame: Bitmap, val aspectRation: String) : UiState()
        data class HlsStreamReady(val uri: Uri, val aspectRation: String) : UiState()
        data class Error(val isManualReconnect: Boolean, val streamUrl: String? = null) : UiState()
    }
}