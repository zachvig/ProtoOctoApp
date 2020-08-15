package de.crysxd.octoapp.base.ui.widget.webcam

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.livedata.OctoTransformations.filterEventsForMessageType
import de.crysxd.octoapp.base.ui.BaseViewModel
import de.crysxd.octoapp.base.usecase.GetWebcamSettingsUseCase
import de.crysxd.octoapp.octoprint.models.settings.WebcamSettings
import de.crysxd.octoapp.octoprint.models.socket.Message
import timber.log.Timber


class WebcamWidgetViewModel(
    val octoPrintProvider: OctoPrintProvider,
    val getWebcamSettingsUseCase: GetWebcamSettingsUseCase
) : BaseViewModel() {


    private var previousSource: LiveData<UiState>? = null
    private val uiStateMediator = MediatorLiveData<UiState>()
    val uiState = uiStateMediator.map { it }
    private val settingsUpdatedLiveData = octoPrintProvider.eventLiveData
        .filterEventsForMessageType(Message.EventMessage.SettingsUpdated::class.java)

    init {
        uiStateMediator.addSource(octoPrintProvider.octoPrint) { connect() }
        uiStateMediator.addSource(settingsUpdatedLiveData) { connect() }
        uiStateMediator.postValue(UiState.Loading)
        connect()
    }

    fun connect() {
        previousSource?.let(uiStateMediator::removeSource)

        val liveData = liveData {
            try {
                emit(UiState.Loading)

                // Load settings
                val octoPrint = octoPrintProvider.octoPrint.value ?: return@liveData emit(UiState.Error(true))
                val webcamSettings = getWebcamSettingsUseCase.execute(GetWebcamSettingsUseCase.Params(octoPrint))

                // Check if webcam is configured
                if (!webcamSettings.webcamEnabled || webcamSettings.streamUrl.isEmpty()) {
                    return@liveData emit(UiState.WebcamNotConfigured)
                }

                // Open stream
                emitSource(MjpegLiveData(webcamSettings.streamUrl).map {
                    when (it) {
                        is MjpegLiveData.MjpegSnapshot.Loading -> UiState.Loading
                        is MjpegLiveData.MjpegSnapshot.Error -> UiState.Error(false, webcamSettings.streamUrl)
                        is MjpegLiveData.MjpegSnapshot.Frame -> UiState.FrameReady(applyTransformations(it.frame, webcamSettings))
                    }
                })
            } catch (e: Exception) {
                Timber.e(e)
                emit(UiState.Error(true))
            }
        }

        previousSource = liveData
        uiStateMediator.addSource(liveData) { uiStateMediator.postValue(it) }
    }

    private fun applyTransformations(frame: Bitmap, webcamSettings: WebcamSettings) = if (webcamSettings.flipV || webcamSettings.flipH || webcamSettings.rotate90) {
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
        data class FrameReady(val frame: Bitmap) : UiState()
        data class Error(val isManualReconnect: Boolean, val streamUrl: String? = null) : UiState()
    }
}