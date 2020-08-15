package de.crysxd.octoapp.print_controls.ui.widget

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import de.crysxd.octoapp.base.ui.BaseViewModel
import de.crysxd.octoapp.base.usecase.TunePrintUseCase
import kotlinx.coroutines.launch

const val KEY_SHOW_DATA_HINT = "show_data_hint"

class TuneFragmentViewModel(
    private val sharedPreferences: SharedPreferences,
    private val tunePrintUseCase: TunePrintUseCase
) : BaseViewModel() {

    private val mutableUiState = MutableLiveData<UiState>(
        UiState(
            showDataHint = sharedPreferences.getBoolean(KEY_SHOW_DATA_HINT, true),
            initialValue = true
        )
    )
    val uiState = mutableUiState.map { it }

    fun hideDataHint() {
        sharedPreferences.edit { putBoolean(KEY_SHOW_DATA_HINT, false) }
        mutableUiState.postValue(UiState(false))
    }

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
        val showDataHint: Boolean = false,
        val initialValue: Boolean = false,
        val loading: Boolean = false,
        val operationCompleted: Boolean = false
    )
}