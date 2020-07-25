package de.crysxd.octoapp.base.ui.widget.webcam

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.ui.BaseViewModel
import de.crysxd.octoapp.octoprint.models.settings.WebcamSettings
import timber.log.Timber


class WebcamWidgetViewModel(
    val octoPrintProvider: OctoPrintProvider
) : BaseViewModel() {


    private var previousSource: LiveData<UiState>? = null
    private val uiStateMediator = MediatorLiveData<UiState>()
    val uiState = uiStateMediator.map { it }

    init {
        uiStateMediator.addSource(octoPrintProvider.octoPrint) { connect() }
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
                val webcamSettings = octoPrint.createSettingsApi().getSettings().webcam

                // Open stream
                emitSource(MjpegLiveData(webcamSettings.streamUrl).map {
                    when (it) {
                        is MjpegLiveData.MjpegSnapshot.Loading -> UiState.Loading
                        is MjpegLiveData.MjpegSnapshot.Error -> UiState.Error(false)
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
        data class FrameReady(val frame: Bitmap) : UiState()
        data class Error(val isManualReconnect: Boolean) : UiState()
    }
}