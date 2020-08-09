package de.crysxd.octoapp.print_controls.ui.widget.tune

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map

class TuneWidgetViewModel : ViewModel() {

    private val mutableUiState = MutableLiveData(UiState())
    val uiState = mutableUiState.map { it }

    data class UiState(
        val flowRate: Int = 100,
        val feedRate: Int = 100,
        val fanSpeed: Int = 0
    )
}