package de.crysxd.octoapp.base.ui.widget.webcam

import android.graphics.Bitmap
import android.net.Uri
import android.widget.ImageView
import androidx.lifecycle.*
import de.crysxd.octoapp.base.OctoPreferences
import de.crysxd.octoapp.base.billing.BillingManager
import de.crysxd.octoapp.base.ext.isHlsStreamUrl
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.base.ui.base.BaseViewModel
import de.crysxd.octoapp.base.usecase.GetWebcamSettingsUseCase
import de.crysxd.octoapp.base.usecase.HandleAutomaticIlluminationEventUseCase
import de.crysxd.octoapp.octoprint.models.settings.WebcamSettings
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber

@Suppress("EXPERIMENTAL_API_USAGE")
class WebcamViewModel(
    private val octoPrintRepository: OctoPrintRepository,
    private val octoPreferences: OctoPreferences,
    private val getWebcamSettingsUseCase: GetWebcamSettingsUseCase,
    private val handleAutomaticIlluminationEventUseCase: HandleAutomaticIlluminationEventUseCase
) : BaseViewModel() {

    companion object {
        private var instanceCounter = 0
        const val INITIAL_WEBCAM_HASH = -1
        const val FALLBACK_WEBCAM_HASH = 0
    }

    private val tag = "WebcamViewModel/${instanceCounter++}"
    private var previousSource: LiveData<UiState>? = null
    private val uiStateMediator = MediatorLiveData<UiState>()
    private var connectedWebcamSettingsHash: Int = INITIAL_WEBCAM_HASH
    val uiState = uiStateMediator.map { it }
    val connectionCache: Pair<Int, MjpegConnection>? = null
    private val octoPrintLiveData = octoPrintRepository.instanceInformationFlow()
        .filter {
            val result = try {
                // Only pass if changes since last connection call
                getWebcamSettings().hashCode()
            } catch (e: Exception) {
                Timber.tag(tag).e(e)
                FALLBACK_WEBCAM_HASH
            } != connectedWebcamSettingsHash
            Timber.tag(tag).i("Configuration change is relevant: $result")
            result
        }
        .asLiveData()

    init {
        uiStateMediator.addSource(octoPrintLiveData) { connect() }
        uiStateMediator.postValue(UiState.Loading(false))
    }

    private suspend fun getWebcamSettings(): Pair<WebcamSettings?, Int> {
        // Load settings
        val activeWebcamIndex = octoPrintRepository.getActiveInstanceSnapshot()?.appSettings?.activeWebcamIndex ?: 0
        val allWebcamSettings = getWebcamSettingsUseCase.execute(null)
        val preferredSettings = allWebcamSettings?.getOrNull(activeWebcamIndex)
        val webcamSettings = preferredSettings ?: allWebcamSettings?.firstOrNull()

        if (preferredSettings == null) {
            switchWebcam(0)
        }

        return webcamSettings to (allWebcamSettings?.size ?: 0)
    }

    fun connect() {
        Timber.tag(tag).i("Connecting")
        previousSource?.let(uiStateMediator::removeSource)

        val liveData = BillingManager.billingFlow()
            .distinctUntilChangedBy { it.isPremiumActive }
            .map {
                flow {
                    try {
                        emit(UiState.Loading(false))

                        val combinedSettings = getWebcamSettings()
                        connectedWebcamSettingsHash = combinedSettings.hashCode()
                        val (webcamSettings, webcamCount) = combinedSettings
                        val streamUrl = webcamSettings?.streamUrl
                        val authHeader = webcamSettings?.authHeader
                        val canSwitchWebcam = webcamCount > 1
                        Timber.tag(tag).i("Refresh with streamUrl: $streamUrl")
                        Timber.tag(tag).i("Webcam count: $webcamCount")
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
                                emit(
                                    UiState.HlsStreamReady(
                                        uri = Uri.parse(streamUrl),
                                        aspectRation = webcamSettings.streamRatio,
                                        canSwitchWebcam = canSwitchWebcam,
                                        authHeader = authHeader
                                    )
                                )
                            }
                        } else {
                            delay(100)
                            if (octoPreferences.experimentalWebcam) {
                                MjpegConnection2(streamUrl = streamUrl, authHeader = authHeader, name = tag).load()
                            } else {
                                MjpegConnection(streamUrl = streamUrl, authHeader = authHeader, name = tag).load()
                            }.map {
                                when (it) {
                                    is MjpegConnection.MjpegSnapshot.Loading -> UiState.Loading(canSwitchWebcam)
                                    is MjpegConnection.MjpegSnapshot.Frame -> UiState.FrameReady(
                                        frame = it.frame,
                                        aspectRation = webcamSettings.streamRatio,
                                        canSwitchWebcam = canSwitchWebcam,
                                        flipV = webcamSettings.flipV,
                                        flipH = webcamSettings.flipH,
                                        rotate90 = webcamSettings.rotate90,
                                    )
                                }
                            }.catch {
                                Timber.tag(tag).i("ERROR")
                                Timber.e(it)
                                emit(
                                    UiState.Error(
                                        isManualReconnect = true,
                                        streamUrl = webcamSettings.streamUrl,
                                        aspectRation = webcamSettings.streamRatio,
                                        canSwitchWebcam = canSwitchWebcam,
                                    )
                                )
                            }.onStart {
                                handleAutomaticIlluminationEventUseCase.execute(HandleAutomaticIlluminationEventUseCase.Event.WebcamVisible)
                            }.onCompletion {
                                // Switch to global scope as the webcam stream scrop is dead and will not allow sending any network requests
                                GlobalScope.launch {
                                    handleAutomaticIlluminationEventUseCase.execute(HandleAutomaticIlluminationEventUseCase.Event.WebcamGone)
                                }
                            }.collect {
                                emit(it)
                            }
                        }
                    } catch (e: CancellationException) {
                        Timber.tag(tag).w("Webcam stream cancelled")
                    } catch (e: Exception) {
                        Timber.e(e)
                        emit(UiState.Error(true, canSwitchWebcam = false))
                    }
                }.flowOn(Dispatchers.IO)
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
            Timber.tag(tag).i("Switching to webcam $index")
            it.copy(activeWebcamIndex = activeIndex)
        }
    }

    fun getScaleType(isFullscreen: Boolean, default: ImageView.ScaleType) = ImageView.ScaleType.values()[
            octoPrintRepository.getActiveInstanceSnapshot()?.appSettings?.let {
                if (isFullscreen) it.webcamFullscreenScaleType else it.webcamScaleType
            } ?: default.ordinal
    ]

    fun getInitialAspectRatio() = octoPrintRepository.getActiveInstanceSnapshot()?.settings?.webcam?.streamRatio ?: "16:9"

    sealed class UiState(open val canSwitchWebcam: Boolean) {
        data class Loading(override val canSwitchWebcam: Boolean) : UiState(canSwitchWebcam)
        object WebcamNotConfigured : UiState(false)
        data class HlsStreamDisabled(override val canSwitchWebcam: Boolean) : UiState(canSwitchWebcam)
        data class FrameReady(
            val frame: Bitmap,
            val aspectRation: String,
            override val canSwitchWebcam: Boolean,
            val flipH: Boolean,
            val flipV: Boolean,
            val rotate90: Boolean,
        ) : UiState(canSwitchWebcam)

        data class HlsStreamReady(val uri: Uri, val authHeader: String?, val aspectRation: String, override val canSwitchWebcam: Boolean) : UiState(canSwitchWebcam)
        data class Error(val isManualReconnect: Boolean, val streamUrl: String? = null, val aspectRation: String? = null, override val canSwitchWebcam: Boolean) :
            UiState(canSwitchWebcam)
    }
}
