package de.crysxd.octoapp.filemanager.ui.file_details

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import de.crysxd.baseui.BaseViewModel
import de.crysxd.baseui.utils.NavigationResultMediator
import de.crysxd.octoapp.base.network.OctoPrintProvider
import de.crysxd.octoapp.base.usecase.StartPrintJobUseCase
import de.crysxd.octoapp.octoprint.models.files.FileObject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Suppress("EXPERIMENTAL_API_USAGE")
class FileDetailsViewModel(
    private val startPrintJobUseCase: StartPrintJobUseCase,
    private val octoPrintProvider: OctoPrintProvider,
) : BaseViewModel() {

    lateinit var file: FileObject.File

    private val mutableViewEvents = MutableLiveData<ViewEvent>()
    private val mutableLoading = MutableLiveData(false)
    val loading = mutableLoading.map { it }
    val viewEvents = mutableViewEvents.map { it }
    val canStartPrint = octoPrintProvider.passiveCurrentMessageFlow("file-details").map {
        it.state?.flags?.isPrinting() == false
    }.asLiveData()

    fun startPrint(
        materialSelectionConfirmed: Boolean = false,
        timelapseConfigConfirmed: Boolean = false
    ) = viewModelScope.launch(coroutineExceptionHandler) {
        mutableLoading.postValue(true)

        try {
            val result = startPrintJobUseCase.execute(
                StartPrintJobUseCase.Params(
                    file = file,
                    materialSelectionConfirmed = materialSelectionConfirmed,
                    timelapseConfigConfirmed = timelapseConfigConfirmed
                )
            )

            when (result) {
                StartPrintJobUseCase.Result.MaterialSelectionRequired -> requireMaterialSelectionAndStart(timelapseConfigConfirmed)
                StartPrintJobUseCase.Result.TimelapseConfigRequired -> requireTimelapseConfigAndStart(materialSelectionConfirmed)
                StartPrintJobUseCase.Result.PrintStarted -> mutableViewEvents.postValue(ViewEvent.PrintStarted())
            }
        } catch (e: Exception) {
            // Disable loading state on error, but keep on success as we will be navigated away
            mutableLoading.postValue(false)
        }
    }

    private suspend fun requireTimelapseConfigAndStart(materialSelectionConfirmed: Boolean) {
        val (resultId, liveData) = NavigationResultMediator.registerResultCallback<Boolean>()
        mutableViewEvents.postValue(ViewEvent.TimelapseConfigRequired(resultId))
        if (liveData.asFlow().first() == true) {
            startPrint(timelapseConfigConfirmed = true, materialSelectionConfirmed = materialSelectionConfirmed)
        }
    }

    private suspend fun requireMaterialSelectionAndStart(timelapseConfigConfirmed: Boolean) {
        val (resultId, liveData) = NavigationResultMediator.registerResultCallback<Boolean>()
        mutableViewEvents.postValue(ViewEvent.MaterialSelectionRequired(resultId))
        if (liveData.asFlow().first() == true) {
            startPrint(timelapseConfigConfirmed = timelapseConfigConfirmed, materialSelectionConfirmed = true)
        }
    }

    sealed class ViewEvent {
        var isConsumed = false

        data class MaterialSelectionRequired(val resultId: Int) : ViewEvent()
        data class TimelapseConfigRequired(val resultId: Int) : ViewEvent()
        class PrintStarted : ViewEvent()
    }
}
