package de.crysxd.baseui.widget.webcam

import android.content.Context
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
import de.crysxd.octoapp.base.network.OctoPrintProvider
import de.crysxd.octoapp.base.usecase.GetWebcamSettingsUseCase
import de.crysxd.octoapp.base.usecase.HandleAutomaticLightEventUseCase
import de.crysxd.octoapp.base.usecase.ShareImageUseCase
import de.crysxd.octoapp.octoprint.models.settings.WebcamSettings
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Suppress("EXPERIMENTAL_API_USAGE")
class WebcamViewModel(
    private val octoPrintRepository: OctoPrintRepository,
    private val octoPrintProvider: OctoPrintProvider,
    private val octoPreferences: OctoPreferences,
    private val shareImageUseCase: ShareImageUseCase,
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
    private var webcamCount = 1

    init {
        uiStateMediator.addSource(settingsLiveData) { connect() }
        uiStateMediator.postValue(UiState.Loading(false))
    }

    private suspend fun getWebcamSettings(): Flow<Pair<ResolvedWebcamSettings?, Int>> {
        // Load settings
        return getWebcamSettingsUseCase.execute(null)
            .combine(octoPrintRepository.instanceInformationFlow()) { ws, info ->
                val activeWebcamIndex = info?.appSettings?.activeWebcamIndex ?: 0
                val preferredSettings = ws.getOrNull(activeWebcamIndex)
                val webcamSettings = preferredSettings ?: ws.firstOrNull()
                webcamCount = ws.size

                if (preferredSettings == null) {
                    switchWebcam(0)
                }

                webcamSettings to (ws.size)
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
                    canSwitchWebcam = canSwitchWebcam,
                    authHeader = basicAuth,
                    flipV = webcamSettings.flipV,
                    flipH = webcamSettings.flipH,
                    rotate90 = webcamSettings.rotate90,
                    enforcedAspectRatio = webcamSettings.streamRatio.takeIf { octoPreferences.isAspectRatioFromOctoPrint },
                )
            )
        }
    }

    private val OctoPreferences.isAspectRatioFromOctoPrint get() = webcamAspectRatioSource == OctoPreferences.VALUE_WEBCAM_ASPECT_RATIO_SOURCE_OCTOPRINT

    private suspend fun FlowCollector<UiState>.emitMjpegFlow(mjpegSettings: ResolvedWebcamSettings.MjpegSettings, canSwitchWebcam: Boolean) {
        delay(100)
        MjpegConnection2(streamUrl = mjpegSettings.url, name = tag).load().map {
            when (it) {
                is MjpegConnection2.MjpegSnapshot.Loading -> UiState.Loading(canSwitchWebcam)
                is MjpegConnection2.MjpegSnapshot.Frame -> UiState.FrameReady(
                    frame = it.frame,
                    canSwitchWebcam = canSwitchWebcam,
                    flipV = mjpegSettings.webcamSettings.flipV,
                    flipH = mjpegSettings.webcamSettings.flipH,
                    rotate90 = mjpegSettings.webcamSettings.rotate90,
                    enforcedAspectRatio = mjpegSettings.webcamSettings.streamRatio.takeIf { octoPreferences.isAspectRatioFromOctoPrint },
                )
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

    fun storeAspectRatio(ratio: String) = viewModelScope.launch {
        octoPrintRepository.updateAppSettingsForActive {
            it.copy(webcamLastAspectRatio = ratio)
        }
    }

    fun nextWebcam() = switchWebcam(null)

    private fun switchWebcam(index: Int?) = viewModelScope.launch(coroutineExceptionHandler) {
        octoPrintRepository.updateAppSettingsForActive {
            val activeIndex = index ?: (it.activeWebcamIndex + 1) % webcamCount
            Timber.tag(tag).i("Switching to webcam $activeIndex")
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

    fun shareImage(context: Context, imageFactory: suspend () -> Bitmap?) = viewModelScope.launch(Dispatchers.Default + coroutineExceptionHandler) {
        val octoPrint = octoPrintRepository.getActiveInstanceSnapshot()
        val current = octoPrintProvider.passiveCurrentMessageFlow("share-image").firstOrNull()
        val isPrinting = current?.state?.flags?.isPrinting() == true
        val imageName = listOfNotNull(
            octoPrint?.label,
            current?.job?.file?.name?.takeIf { isPrinting }?.split(".")?.firstOrNull(),
            current?.progress?.let { "${it.completion.roundToInt()}percent" }?.takeIf { isPrinting },
            SimpleDateFormat("yyyy-MM-dd__hh-mm-ss", Locale.ENGLISH).format(Date())
        ).joinToString("__")
        val bitmap = imageFactory() ?: return@launch
        shareImageUseCase.execute(ShareImageUseCase.Params(context = context, bitmap = bitmap, imageName = imageName))
    }

    sealed class UiState(open val canSwitchWebcam: Boolean) {
        object WebcamNotConfigured : UiState(false)

        data class Loading(
            override val canSwitchWebcam: Boolean
        ) : UiState(canSwitchWebcam)

        data class RichStreamDisabled(
            override val canSwitchWebcam: Boolean
        ) : UiState(canSwitchWebcam)

        data class Error(
            val isManualReconnect: Boolean,
            val streamUrl: String? = null,
            override val canSwitchWebcam: Boolean
        ) : UiState(canSwitchWebcam)

        data class RichStreamReady(
            val uri: Uri,
            val authHeader: String?,
            override val canSwitchWebcam: Boolean,
            val flipH: Boolean,
            val flipV: Boolean,
            val rotate90: Boolean,
            val enforcedAspectRatio: String?,
        ) : UiState(canSwitchWebcam)

        data class FrameReady(
            val frame: Bitmap,
            override val canSwitchWebcam: Boolean,
            val flipH: Boolean,
            val flipV: Boolean,
            val rotate90: Boolean,
            val enforcedAspectRatio: String?,
        ) : UiState(canSwitchWebcam)

    }
}
