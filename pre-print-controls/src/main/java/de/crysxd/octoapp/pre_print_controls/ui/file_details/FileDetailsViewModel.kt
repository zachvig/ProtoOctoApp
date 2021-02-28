package de.crysxd.octoapp.pre_print_controls.ui.file_details

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import de.crysxd.octoapp.base.ui.base.BaseViewModel
import de.crysxd.octoapp.base.usecase.StartPrintJobUseCase
import de.crysxd.octoapp.octoprint.models.files.FileObject
import kotlinx.coroutines.launch

@Suppress("EXPERIMENTAL_API_USAGE")
class FileDetailsViewModel(
    private val startPrintJobUseCase: StartPrintJobUseCase,
) : BaseViewModel() {

    lateinit var file: FileObject.File

    private val mutableViewEvents = MutableLiveData<ViewEvent>()
    private val mutableLoading = MutableLiveData(false)
    val loading = mutableLoading.map { it }
    val viewEvents = mutableViewEvents.map { it }

    fun startPrint() = viewModelScope.launch(coroutineExceptionHandler) {
        mutableLoading.postValue(true)

        try {
            val result = startPrintJobUseCase.execute(StartPrintJobUseCase.Params(file = file, materialSelectionConfirmed = false))
            if (result == StartPrintJobUseCase.Result.MaterialSelectionRequired) {
                mutableViewEvents.postValue(ViewEvent.MaterialSelectionRequired())
            }
        } finally {
            mutableLoading.postValue(false)
        }
    }

    sealed class ViewEvent {
        var isConsumed = false

        class MaterialSelectionRequired : ViewEvent()
    }
}
