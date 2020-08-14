package de.crysxd.octoapp.print_controls.ui.widget

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map

const val KEY_SHOW_DATA_HINT = "show_data_hint"

class TuneFragmentViewModel(
    private val sharedPreferences: SharedPreferences
) : ViewModel() {

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

    data class UiState(
        val showDataHint: Boolean = false,
        val initialValue: Boolean = false
    )
}