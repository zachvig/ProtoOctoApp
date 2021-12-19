package de.crysxd.baseui.timelapse

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import de.crysxd.baseui.BaseViewModel
import de.crysxd.octoapp.base.data.repository.TimelapseRepository
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber

class TimelapseArchiveViewModel(
    private val timelapseRepository: TimelapseRepository
) : BaseViewModel() {

    val viewData = timelapseRepository.flow().map { it?.files ?: emptyList() }.asLiveData()
    private val mutableViewState = MutableLiveData<ViewState>()
    val viewState = mutableViewState.map { it }

    init {
        if (timelapseRepository.peek() == null) {
            fetchLatest()
        }
    }

    fun fetchLatest() = viewModelScope.launch(coroutineExceptionHandler) {
        try {
            mutableViewState.postValue(ViewState.Loading)
            timelapseRepository.fetchLatest()
            mutableViewState.postValue(ViewState.Idle)
        } catch (e: Exception) {
            Timber.e(e)
            mutableViewState.postValue(ViewState.Error(e))
        }
    }

    sealed class ViewState {
        object Loading : ViewState()
        object Idle : ViewState()
        data class Error(val exception: Exception) : ViewState()
    }
}