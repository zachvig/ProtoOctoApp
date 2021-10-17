package de.crysxd.octoapp.printcontrols.ui.widget

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import de.crysxd.baseui.BaseViewModel
import de.crysxd.octoapp.base.usecase.ExecuteGcodeCommandUseCase
import de.crysxd.octoapp.base.usecase.TunePrintUseCase
import de.crysxd.octoapp.octoprint.models.printer.GcodeCommand
import kotlinx.coroutines.launch
import timber.log.Timber


class TuneFragmentViewModel(
    private val tunePrintUseCase: TunePrintUseCase,
    private val executeGcodeCommandUseCase: ExecuteGcodeCommandUseCase,
) : BaseViewModel() {

    private val mutableUiState = MutableLiveData(UiState(initialValue = true))
    val uiState = mutableUiState.map { it }

    fun babyStepUp() = doBabyStep(0.025f)

    fun babyStepDown() = doBabyStep(-0.025f)

    private fun doBabyStep(distanceMm: Float) = viewModelScope.launch(coroutineExceptionHandler) {
        Timber.i("Performing baby step: $distanceMm")
        val params = ExecuteGcodeCommandUseCase.Param(
            command = GcodeCommand.Single("M290 Z$distanceMm"),
            recordResponse = false,
            fromUser = false
        )
        executeGcodeCommandUseCase.execute(params)
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
        val initialValue: Boolean = false,
        val loading: Boolean = false,
        val operationCompleted: Boolean = false
    )
}