package de.crysxd.octoapp.pre_print_controls.ui.file_details

import androidx.lifecycle.viewModelScope
import de.crysxd.octoapp.base.ui.BaseViewModel
import de.crysxd.octoapp.base.usecase.StartPrintJobUseCase
import de.crysxd.octoapp.octoprint.models.files.FileObject
import kotlinx.coroutines.launch

class FileDetailsViewModel(
    private val startPrintJobUseCase: StartPrintJobUseCase
) : BaseViewModel() {

    lateinit var file: FileObject.File

    fun startPrint(file: FileObject.File) = viewModelScope.launch(coroutineExceptionHandler) {
        startPrintJobUseCase.execute(file)
    }
}
