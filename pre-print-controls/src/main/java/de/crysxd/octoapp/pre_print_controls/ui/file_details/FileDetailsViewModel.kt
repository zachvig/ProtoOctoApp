package de.crysxd.octoapp.pre_print_controls.ui.file_details

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import de.crysxd.octoapp.base.livedata.OctoTransformations.map
import de.crysxd.octoapp.base.ui.BaseViewModel
import de.crysxd.octoapp.base.usecase.StartPrintJobUseCase
import de.crysxd.octoapp.octoprint.models.files.FileObject
import kotlinx.coroutines.launch

@Suppress("EXPERIMENTAL_API_USAGE")
class FileDetailsViewModel(
    private val startPrintJobUseCase: StartPrintJobUseCase,
) : BaseViewModel() {

    lateinit var file: FileObject.File

    private val mutableLoading = MutableLiveData(false)
    val loading = mutableLoading.map { it }

    fun startPrint() = viewModelScope.launch(coroutineExceptionHandler) {
        mutableLoading.postValue(true)
        try {
            startPrintJobUseCase.execute(file)
        } finally {
            mutableLoading.postValue(false)
        }
    }
}
