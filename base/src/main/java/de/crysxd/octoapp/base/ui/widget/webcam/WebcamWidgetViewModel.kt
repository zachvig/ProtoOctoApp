package de.crysxd.octoapp.base.ui.widget.webcam

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.ui.BaseViewModel
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
                        is MjpegLiveData.MjpegSnapshot.Frame -> UiState.FrameReady(it.frame)
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

    sealed class UiState {
        object Loading : UiState()
        data class FrameReady(val frame: Bitmap) : UiState()
        data class Error(val isManualReconnect: Boolean) : UiState()
    }
}