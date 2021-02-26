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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
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
        .map { getWebcamSettings().first?.streamUrl }
        .distinctUntilChangedBy { it }
        .asLiveData()

    init {
        uiStateMediator.addSource(octoPrintLiveData) { connect() }
        uiStateMediator.postValue(UiState.Loading(false))
    }

    private suspend fun getWebcamSettings(): Pair<WebcamSettings?, Int> {
        // Load settings
        val activeWebcamIndex = octoPrintRepository.getActiveInstanceSnapshot()?.appSettings?.activeWebcamIndex ?: 0
        val allWebcamSettings = getWebcamSettingsUseCase.execute(null)
        val preferredSettings = allWebcamSettings.getOrNull(activeWebcamIndex)
        val webcamSettings = preferredSettings ?: allWebcamSettings.firstOrNull()

        if (preferredSettings == null) {
            switchWebcam(0)
        }

        return webcamSettings to allWebcamSettings.size
    }

    fun connect() {
        Timber.i("Connecting")
        previousSource?.let(uiStateMediator::removeSource)

        val liveData = BillingManager.billingFlow().map {
            flow {
                try {
                    emit(UiState.Loading(false))

                    val (webcamSettings, webcamCount) = getWebcamSettings()
                    val streamUrl = webcamSettings?.streamUrl
                    val canSwitchWebcam = webcamCount > 1
                    Timber.i("Refresh with streamUrl: $streamUrl")
                    emit(UiState.Loading(canSwitchWebcam))

                    // Check if webcam is configured
                    if (webcamSettings?.webcamEnabled == false || streamUrl.isNullOrBlank()) {
                        return@flow emit(UiState.WebcamNotConfigured)
                    }

                    // Open stream
                    if (streamUrl.isHlsStreamUrl) {
                        if (!BillingManager.isFeatureEnabled("hls_webcam")) {
                            emit(UiState.HlsStreamDisabled(canSwitchWebcam = canSwitchWebcam))
                        } else {
                            emit(UiState.HlsStreamReady(Uri.parse(streamUrl), webcamSettings.streamRatio, canSwitchWebcam = canSwitchWebcam))
                        }
                    } else {
                        MjpegConnection(streamUrl, "vm")
                            .load()
                            .map {
                                when (it) {
                                    is MjpegConnection.MjpegSnapshot.Loading -> UiState.Loading(canSwitchWebcam)
                                    is MjpegConnection.MjpegSnapshot.Frame -> UiState.FrameReady(
                                        frame = it.lock.withLock { applyTransformations(it.frame, webcamSettings) },
                                        aspectRation = webcamSettings.streamRatio,
                                        canSwitchWebcam = canSwitchWebcam,
                                    )
                                }
                            }
                            .flowOn(Dispatchers.Default)
                            .catch {
                                Timber.i("ERROR")
                                Timber.e(it)
                                emit(
                                    UiState.Error(
                                        isManualReconnect = true,
                                        streamUrl = webcamSettings.streamUrl,
                                        aspectRation = webcamSettings.streamRatio,
                                        canSwitchWebcam = canSwitchWebcam,
                                    )
                                )
                            }
                            .collect {
                                emit(it)
                            }
                    }
                } catch (e: CancellationException) {
                    Timber.w("Webcam stream cancelled")
                } catch (e: Exception) {
                    Timber.e(e)
                    emit(UiState.Error(true, canSwitchWebcam = false))
                }
            }
        }.flatMapLatest { it }.asLiveData()

        previousSource = liveData
        uiStateMediator.addSource(liveData)
        { uiStateMediator.postValue(it) }
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

    fun nextWebcam() = switchWebcam(null)

    private fun switchWebcam(index: Int?) = viewModelScope.launch(coroutineExceptionHandler) {
        octoPrintRepository.updateAppSettingsForActive {
            val activeIndex = index ?: it.activeWebcamIndex + 1
            Timber.i("Switching to webcam $index")
            it.copy(activeWebcamIndex = activeIndex)
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

    sealed class UiState(open val canSwitchWebcam: Boolean) {
        data class Loading(override val canSwitchWebcam: Boolean) : UiState(canSwitchWebcam)
        object WebcamNotConfigured : UiState(false)
        data class HlsStreamDisabled(override val canSwitchWebcam: Boolean) : UiState(canSwitchWebcam)
        data class FrameReady(val frame: Bitmap, val aspectRation: String, override val canSwitchWebcam: Boolean) : UiState(canSwitchWebcam)
        data class HlsStreamReady(val uri: Uri, val aspectRation: String, override val canSwitchWebcam: Boolean) : UiState(canSwitchWebcam)
        data class Error(val isManualReconnect: Boolean, val streamUrl: String? = null, val aspectRation: String? = null, override val canSwitchWebcam: Boolean) :
            UiState(canSwitchWebcam)
    }
}
