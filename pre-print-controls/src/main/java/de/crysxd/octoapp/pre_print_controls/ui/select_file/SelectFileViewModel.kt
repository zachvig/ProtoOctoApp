package de.crysxd.octoapp.pre_print_controls.ui.select_file

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewModelScope
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.ui.BaseViewModel
import de.crysxd.octoapp.base.usecase.LoadFilesUseCase
import de.crysxd.octoapp.base.usecase.StartPrintJobUseCase
import de.crysxd.octoapp.octoprint.models.files.FileObject
import de.crysxd.octoapp.octoprint.models.files.FileOrigin
import de.crysxd.octoapp.pre_print_controls.R
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SelectFileViewModel(
    private val octoPrintProvider: OctoPrintProvider,
    private val loadFilesUseCase: LoadFilesUseCase,
    private val startPrintJobUseCase: StartPrintJobUseCase
) : BaseViewModel() {

    val rootFilesMediator = MediatorLiveData<List<FileObject>>()
    private var rootFilesInitialised = false

    fun loadRootFiles(): LiveData<List<FileObject>> {
        if (!rootFilesInitialised) {
            rootFilesInitialised = true
            rootFilesMediator.addSource(octoPrintProvider.octoPrint) {
                viewModelScope.launch(coroutineExceptionHandler) {
                    it?.let {
                        val root = loadFilesUseCase.execute(Triple(it, FileOrigin.Local, null))
                        rootFilesMediator.postValue(root)
                        val test = loadFilesUseCase.execute(Triple(it, FileOrigin.Local, root.first { it is FileObject.Folder } as FileObject.Folder))
                        rootFilesMediator.postValue(test)
                    }
                }
            }
        }

        return Transformations.map(rootFilesMediator) { it }
    }

    fun selectFile(file: FileObject) = GlobalScope.launch(coroutineExceptionHandler) {
        when (file) {
            is FileObject.File -> {
                octoPrintProvider.octoPrint.value?.let {
                    startPrintJobUseCase.execute(Pair(it, file))
                }
            }
            is FileObject.Folder -> {
                navContoller.navigate(R.id.action_open_folder, SelectFileFragmentArgs(file).toBundle())
            }
        }
    }
}
