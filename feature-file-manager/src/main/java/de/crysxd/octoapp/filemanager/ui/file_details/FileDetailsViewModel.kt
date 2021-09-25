package de.crysxd.octoapp.filemanager.ui.file_details

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import de.crysxd.baseui.BaseViewModel
import de.crysxd.baseui.OctoActivity
import de.crysxd.octoapp.base.network.OctoPrintProvider
import de.crysxd.octoapp.base.usecase.StartPrintJobUseCase
import de.crysxd.octoapp.octoprint.models.files.FileObject
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

    fun startPrint() = viewModelScope.launch(coroutineExceptionHandler) {
        mutableLoading.postValue(true)

        try {
            val result = startPrintJobUseCase.execute(StartPrintJobUseCase.Params(file = file, materialSelectionConfirmed = false))
            if (result == StartPrintJobUseCase.Result.MaterialSelectionRequired) {
                mutableViewEvents.postValue(ViewEvent.MaterialSelectionRequired())
            } else {
                OctoActivity.instance?.enforceAllowAutomaticNavigationFromCurrentDestination()
            }
        } catch (e: Exception) {
            // Disable loading state on error, but keep on success as we will be navigated away
            mutableLoading.postValue(false)
        }
    }

    sealed class ViewEvent {
        var isConsumed = false

        class MaterialSelectionRequired : ViewEvent()
        class PrintStarted : ViewEvent()
    }
}
