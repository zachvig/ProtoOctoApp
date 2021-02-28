package de.crysxd.octoapp.print_controls.ui.widget

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import de.crysxd.octoapp.base.ui.base.BaseViewModel
import de.crysxd.octoapp.base.usecase.TunePrintUseCase
import kotlinx.coroutines.launch


class TuneFragmentViewModel(
    private val tunePrintUseCase: TunePrintUseCase
) : BaseViewModel() {

    private val mutableUiState = MutableLiveData(UiState(initialValue = true))
    val uiState = mutableUiState.map { it }

    fun applyChanges(
        feedRate: Int?,
        flowRate: Int?,
        fanSpeed: Int?
    ) = viewModelScope.launch(coroutineExceptionHandler) {
        mutableUiState.postValue(mutableUiState.value?.copy(loading = true))

        try {
            tunePrintUseCase.execute(
                TunePrintUseCase.Param(
                    feedRate = feedRate,
                    flowRate = flowRate,
                    fanSpeed = fanSpeed
                )
            )
        } finally {
            mutableUiState.postValue(mutableUiState.value?.copy(loading = false))
        }

        mutableUiState.postValue(mutableUiState.value?.copy(operationCompleted = true))
    }

    data class UiState(
        val initialValue: Boolean = false,
        val loading: Boolean = false,
        val operationCompleted: Boolean = false
    )
}