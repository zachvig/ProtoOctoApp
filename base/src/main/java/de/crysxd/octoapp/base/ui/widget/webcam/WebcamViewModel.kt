package de.crysxd.octoapp.base.ui.widget.webcam

import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.widget.ImageView
import androidx.core.content.edit
import androidx.lifecycle.*
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.billing.BillingManager
import de.crysxd.octoapp.base.ext.isHlsStreamUrl
import de.crysxd.octoapp.base.ui.BaseViewModel
import de.crysxd.octoapp.base.usecase.GetWebcamSettingsUseCase
import de.crysxd.octoapp.base.usecase.execute
import de.crysxd.octoapp.octoprint.models.settings.WebcamSettings
import de.crysxd.octoapp.octoprint.models.socket.Event
import de.crysxd.octoapp.octoprint.models.socket.Message.EventMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import timber.log.Timber

private const val KEY_ASPECT_RATIO = "webcam_aspect_ration"
private const val KEY_SCALE_TYPE = "webcam_scale_type"
private const val KEY_SCALE_TYPE_FULLSCREEN = "webcam_scale_type_fullscreen"

@Suppress("EXPERIMENTAL_API_USAGE")
class WebcamViewModel(
    octoPrintProvider: OctoPrintProvider,
    private val getWebcamSettingsUseCase: GetWebcamSettingsUseCase,
    private val sharedPreferences: SharedPreferences
) : BaseViewModel() {

    private var previousSource: LiveData<UiState>? = null
    private val uiStateMediator = MediatorLiveData<UiState>()
    val uiState = uiStateMediator.map { it }
    private val settingsUpdatedLiveData = octoPrintProvider.eventFlow("webcam")
        .filter { it is Event.MessageReceived && it.message is EventMessage.SettingsUpdated }
        .debounce(1000)
        .asLiveData()

    init {
        uiStateMediator.addSource(octoPrintProvider.octoPrintFlow().asLiveData()) { connect() }
        uiStateMediator.addSource(settingsUpdatedLiveData) { connect() }
        uiStateMediator.postValue(UiState.Loading)
    }

    fun connect() {
        previousSource?.let(uiStateMediator::removeSource)

        val liveData = BillingManager.billingFlow().map {
            flow {
                try {
                    emit(UiState.Loading)

                    // Load settings
                    val webcamSettings = getWebcamSettingsUseCase.execute()
                    storeAspectRatio(webcamSettings.streamRatio)
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
                                    is MjpegConnection.MjpegSnapshot.Error -> UiState.Error(
                                        isManualReconnect = false,
                                        streamUrl = webcamSettings.streamUrl
                                    )
                                    is MjpegConnection.MjpegSnapshot.Frame -> UiState.FrameReady(
                                        frame = applyTransformations(it.frame, webcamSettings),
                                        aspectRation = webcamSettings.streamRatio
                                    )
                                }
                            }
                            .flowOn(Dispatchers.Default)
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

    fun storeScaleType(scaleType: ImageView.ScaleType, isFullscreen: Boolean) = sharedPreferences.edit {
        putInt(if (isFullscreen) KEY_SCALE_TYPE_FULLSCREEN else KEY_SCALE_TYPE, scaleType.ordinal)
    }

    fun getScaleType(isFullscreen: Boolean, default: ImageView.ScaleType) = ImageView.ScaleType.values()[sharedPreferences.getInt(
        if (isFullscreen) KEY_SCALE_TYPE_FULLSCREEN else KEY_SCALE_TYPE,
        default.ordinal
    )]

    private fun storeAspectRatio(aspectRatio: String) = sharedPreferences.edit {
        putString(KEY_ASPECT_RATIO, aspectRatio)
    }

    fun getInitialAspectRatio() = sharedPreferences.getString(KEY_ASPECT_RATIO, null) ?: "16:9"

    private fun applyTransformations(frame: Bitmap, webcamSettings: WebcamSettings) =
        if (webcamSettings.flipV || webcamSettings.flipH || webcamSettings.rotate90) {
            val matrix = Matrix()

            if (webcamSettings.rotate90) {
                matrix.postRotate(-90f)
            }

            matrix.postScale(
                if (webcamSettings.flipH) -1f else 1f,
                if (webcamSettings.flipV) -1f else 1f,
                frame.width / 2f,
                frame.height / 2f
            )

            val transformed = Bitmap.createBitmap(frame, 0, 0, frame.width, frame.height, matrix, true)
            frame.recycle()
            transformed
        } else {
            frame
        }

    sealed class UiState {
        object Loading : UiState()
        object WebcamNotConfigured : UiState()
        object HlsStreamDisabled : UiState()
        data class FrameReady(val frame: Bitmap, val aspectRation: String) : UiState()
        data class HlsStreamReady(val uri: Uri, val aspectRation: String) : UiState()
        data class Error(val isManualReconnect: Boolean, val streamUrl: String? = null) : UiState()
    }
}