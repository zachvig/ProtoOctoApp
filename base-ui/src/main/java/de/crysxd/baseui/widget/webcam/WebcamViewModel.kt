package de.crysxd.baseui.widget.webcam

import android.graphics.Bitmap
import android.net.Uri
import android.widget.ImageView
import androidx.lifecycle.*
import de.crysxd.baseui.BaseViewModel
import de.crysxd.octoapp.base.OctoPreferences
import de.crysxd.octoapp.base.billing.BillingManager
import de.crysxd.octoapp.base.billing.BillingManager.FEATURE_HLS_WEBCAM
import de.crysxd.octoapp.base.data.repository.OctoPrintRepository
import de.crysxd.octoapp.base.network.MjpegConnection2
import de.crysxd.octoapp.base.usecase.GetWebcamSettingsUseCase
import de.crysxd.octoapp.base.usecase.HandleAutomaticLightEventUseCase
import de.crysxd.octoapp.octoprint.extractAndRemoveBasicAuth
import de.crysxd.octoapp.octoprint.isHlsStreamUrl
import de.crysxd.octoapp.octoprint.models.settings.WebcamSettings
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber

@Suppress("EXPERIMENTAL_API_USAGE")
class WebcamViewModel(
    private val octoPrintRepository: OctoPrintRepository,
    private val octoPreferences: OctoPreferences,
    private val getWebcamSettingsUseCase: GetWebcamSettingsUseCase,
    private val handleAutomaticLightEventUseCase: HandleAutomaticLightEventUseCase
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
    private val settingsLiveData = octoPreferences.updatedFlow.asLiveData()
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
        uiStateMediator.addSource(settingsLiveData) { connect() }
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
                        val streamUrl = webcamSettings?.absoluteStreamUrl
                        val canSwitchWebcam = webcamCount > 1
                        Timber.tag(tag).i("Refresh with streamUrl: $streamUrl")
                        Timber.tag(tag).i("Webcam count: $webcamCount")
                        emit(UiState.Loading(canSwitchWebcam))

                        // Check if webcam is configured
                        if (webcamSettings?.webcamEnabled == false || streamUrl == null) {
                            return@flow emit(UiState.WebcamNotConfigured)
                        }

                        // Open stream
                        if (streamUrl.isHlsStreamUrl()) {
                            if (!BillingManager.isFeatureEnabled(FEATURE_HLS_WEBCAM)) {
                                emit(UiState.HlsStreamDisabled(canSwitchWebcam = canSwitchWebcam))
                            } else {
                                emit(
                                    UiState.HlsStreamReady(
                                        uri = Uri.parse(streamUrl.toString()),
                                        aspectRation = webcamSettings.saveStreamRatio,
                                        canSwitchWebcam = canSwitchWebcam,
                                        authHeader = streamUrl.extractAndRemoveBasicAuth().second
                                    )
                                )
                            }
                        } else {
                            delay(100)
                            MjpegConnection2(streamUrl = streamUrl, name = tag).load().map {
                                when (it) {
                                    is MjpegConnection2.MjpegSnapshot.Loading -> UiState.Loading(canSwitchWebcam)
                                    is MjpegConnection2.MjpegSnapshot.Frame -> UiState.FrameReady(
                                        frame = it.frame,
                                        aspectRation = when (octoPreferences.webcamAspectRatioSource) {
                                            OctoPreferences.VALUE_WEBCAM_ASPECT_RATIO_SOURCE_IMAGE -> if (webcamSettings.rotate90) {
                                                "${it.frame.height}:${it.frame.width}"
                                            } else {
                                                "${it.frame.width}:${it.frame.height}"
                                            }
                                            else -> webcamSettings.saveStreamRatio
                                        },
                                        canSwitchWebcam = canSwitchWebcam,
                                        flipV = webcamSettings.flipV,
                                        flipH = webcamSettings.flipH,
                                        rotate90 = webcamSettings.rotate90,
                                    )
                                }
                            }.onEach { state ->
                                if (state is UiState.FrameReady) {
                                    octoPrintRepository.updateAppSettingsForActive {
                                        it.copy(webcamLastAspectRatio = state.aspectRation)
                                    }
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
                                handleAutomaticLightEventUseCase.execute(HandleAutomaticLightEventUseCase.Event.WebcamVisible("webcam-vm"))
                            }.onCompletion {
                                // Execute blocking as a normal execute switches threads causing the task never to be done as the current scope
                                // is about to be terminated
                                handleAutomaticLightEventUseCase.executeBlocking(HandleAutomaticLightEventUseCase.Event.WebcamGone("webcam-vm"))
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

    fun getInitialAspectRatio() = octoPrintRepository.getActiveInstanceSnapshot()?.let {
        it.appSettings?.webcamLastAspectRatio ?: it.settings?.webcam?.streamRatio
    } ?: "16:9"

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
