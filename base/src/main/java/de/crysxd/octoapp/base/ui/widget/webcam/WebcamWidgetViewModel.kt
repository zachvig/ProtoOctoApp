package de.crysxd.octoapp.base.ui.widget.webcam

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import de.crysxd.octoapp.base.ui.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WebcamWidgetViewModel(

) : BaseViewModel() {


    private var previousSource: LiveData<UiState>? = null
    private val uiStateMediator = MediatorLiveData<UiState>()
    val uiState = uiStateMediator.map { it }

    init {
        uiStateMediator.postValue(UiState.Loading)
        connect()
    }

    fun connect() {
        val liveData = liveData {
            emit(UiState.Loading)

            try {
                val streamUrl = withContext(Dispatchers.IO) {
                    "http://192.168.1.33:6677/videofeed?username=&password="
                }

                emitSource(MjpegLiveData(streamUrl).map {
                    when (it) {
                        MjpegLiveData.MjpegSnapshot.Error -> UiState.Error
                        MjpegLiveData.MjpegSnapshot.Loading -> UiState.Loading
                        is MjpegLiveData.MjpegSnapshot.Frame -> UiState.FrameReady(it.frame)
                    }
                })

            } catch (e: Exception) {
                emit(UiState.Error)
            }
        }

        previousSource?.let {
            uiStateMediator.removeSource(it)
        }

        uiStateMediator.addSource(liveData) { uiStateMediator.postValue(it) }
        previousSource = liveData
    }

    sealed class UiState {
        object Loading : UiState()
        data class FrameReady(val frame: Bitmap) : UiState()
        object Error : UiState()
    }
}