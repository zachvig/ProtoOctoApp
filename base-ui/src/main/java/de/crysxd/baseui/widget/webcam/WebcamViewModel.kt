package de.crysxd.baseui.widget.webcam

import android.graphics.Bitmap
import android.net.Uri
import android.widget.ImageView
import androidx.lifecycle.*
import de.crysxd.baseui.BaseViewModel
import de.crysxd.octoapp.base.OctoPreferences
import de.crysxd.octoapp.base.billing.BillingManager
import de.crysxd.octoapp.base.billing.BillingManager.FEATURE_HLS_WEBCAM
import de.crysxd.octoapp.base.data.models.ResolvedWebcamSettings
import de.crysxd.octoapp.base.data.repository.OctoPrintRepository
import de.crysxd.octoapp.base.network.MjpegConnection2
import de.crysxd.octoapp.base.usecase.GetWebcamSettingsUseCase
import de.crysxd.octoapp.base.usecase.HandleAutomaticLightEventUseCase
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
    }

    private val tag = "WebcamViewModel/${instanceCounter++}"
    private var previousSource: LiveData<UiState>? = null
    private val uiStateMediator = MediatorLiveData<UiState>()
    val uiState = uiStateMediator.map { it }
    private val settingsLiveData = octoPreferences.updatedFlow.asLiveData()

    init {
        uiStateMediator.addSource(settingsLiveData) { connect() }
        uiStateMediator.postValue(UiState.Loading(false))
    }

    private suspend fun getWebcamSettings(): Flow<Pair<ResolvedWebcamSettings?, Int>> {
        // Load settings
        val activeWebcamIndex = octoPrintRepository.getActiveInstanceSnapshot()?.appSettings?.activeWebcamIndex ?: 0
        return getWebcamSettingsUseCase.execute(null).map {
            val preferredSettings = it.getOrNull(activeWebcamIndex)
            val webcamSettings = preferredSettings ?: it.firstOrNull()

            if (preferredSettings == null) {
                switchWebcam(0)
            }
            webcamSettings to (it.size)
        }.distinctUntilChanged()
    }

    fun connect() {
        viewModelScope.launch(coroutineExceptionHandler) {
            Timber.tag(tag).i("Connecting")
            previousSource?.let(uiStateMediator::removeSource)

            val liveData = BillingManager.billingFlow()
                .distinctUntilChangedBy { it.isPremiumActive }
                .combine(getWebcamSettings()) { _, ws ->
                    flow {
                        try {
                            emit(UiState.Loading(false))
                            val (resolvedSettings, webcamCount) = ws
                            val canSwitchWebcam = webcamCount > 1
                            Timber.tag(tag).i("Refresh with streamUrl: ${resolvedSettings?.urlString}")
                            Timber.tag(tag).i("Webcam count: $webcamCount")
                            emit(UiState.Loading(canSwitchWebcam))

                            // Check if webcam is configured
                            if (resolvedSettings == null || resolvedSettings.webcamSettings.webcamEnabled == false) {
                                return@flow emit(UiState.WebcamNotConfigured)
                            }

                            // Open stream
                            when (resolvedSettings) {
                                is ResolvedWebcamSettings.HlsSettings -> emitRichFlow(
                                    url = resolvedSettings.urlString,
                                    basicAuth = resolvedSettings.basicAuth,
                                    webcamSettings = resolvedSettings.webcamSettings,
                                    canSwitchWebcam = canSwitchWebcam
                                )

                                is ResolvedWebcamSettings.RtspSettings -> emitRichFlow(
                                    url = resolvedSettings.urlString,
                                    basicAuth = resolvedSettings.basicAuth,
                                    webcamSettings = resolvedSettings.webcamSettings,
                                    canSwitchWebcam = canSwitchWebcam
                                )

                                is ResolvedWebcamSettings.MjpegSettings -> emitMjpegFlow(
                                    mjpegSettings = resolvedSettings,
                                    canSwitchWebcam = canSwitchWebcam
                                )
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
    }

    private suspend fun FlowCollector<UiState>.emitRichFlow(url: String, basicAuth: String?, webcamSettings: WebcamSettings, canSwitchWebcam: Boolean) {
        if (!BillingManager.isFeatureEnabled(FEATURE_HLS_WEBCAM)) {
            emit(UiState.RichStreamDisabled(canSwitchWebcam = canSwitchWebcam))
        } else {
            emit(
                UiState.RichStreamReady(
                    uri = Uri.parse(url),
                    aspectRation = webcamSettings.saveStreamRatio,
                    canSwitchWebcam = canSwitchWebcam,
                    authHeader = basicAuth,
                )
            )
        }
    }

    private suspend fun FlowCollector<UiState>.emitMjpegFlow(mjpegSettings: ResolvedWebcamSettings.MjpegSettings, canSwitchWebcam: Boolean) {
        delay(100)
        var lastAspectRatio: String? = null
        MjpegConnection2(streamUrl = mjpegSettings.url, name = tag).load().map {
            when (it) {
                is MjpegConnection2.MjpegSnapshot.Loading -> UiState.Loading(canSwitchWebcam)
                is MjpegConnection2.MjpegSnapshot.Frame -> UiState.FrameReady(
                    frame = it.frame,
                    aspectRation = when (octoPreferences.webcamAspectRatioSource) {
                        OctoPreferences.VALUE_WEBCAM_ASPECT_RATIO_SOURCE_IMAGE -> if (mjpegSettings.webcamSettings.rotate90) {
                            "${it.frame.height}:${it.frame.width}"
                        } else {
                            "${it.frame.width}:${it.frame.height}"
                        }
                        else -> mjpegSettings.webcamSettings.saveStreamRatio
                    },
                    canSwitchWebcam = canSwitchWebcam,
                    flipV = mjpegSettings.webcamSettings.flipV,
                    flipH = mjpegSettings.webcamSettings.flipH,
                    rotate90 = mjpegSettings.webcamSettings.rotate90,
                )
            }
        }.onEach { state ->
            if (state is UiState.FrameReady && state.aspectRation != lastAspectRatio) {
                lastAspectRatio = state.aspectRation
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
                    streamUrl = mjpegSettings.urlString,
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
        data class RichStreamDisabled(override val canSwitchWebcam: Boolean) : UiState(canSwitchWebcam)
        data class RichStreamReady(val uri: Uri, val authHeader: String?, val aspectRation: String, override val canSwitchWebcam: Boolean) : UiState(canSwitchWebcam)
        data class FrameReady(
            val frame: Bitmap,
            val aspectRation: String,
            override val canSwitchWebcam: Boolean,
            val flipH: Boolean,
            val flipV: Boolean,
            val rotate90: Boolean,
        ) : UiState(canSwitchWebcam)

        data class Error(val isManualReconnect: Boolean, val streamUrl: String? = null, override val canSwitchWebcam: Boolean) :
            UiState(canSwitchWebcam)
    }
}
