package de.crysxd.octoapp.pre_print_controls.ui.select_file

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewModelScope
import com.squareup.picasso.Picasso
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.ui.BaseViewModel
import de.crysxd.octoapp.base.usecase.LoadFilesUseCase
import de.crysxd.octoapp.base.usecase.LoadFilesUseCase.Params
import de.crysxd.octoapp.base.usecase.StartPrintJobUseCase
import de.crysxd.octoapp.octoprint.models.files.FileObject
import de.crysxd.octoapp.octoprint.models.files.FileOrigin
import de.crysxd.octoapp.pre_print_controls.R
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SelectFileViewModel(
    private val octoPrintProvider: OctoPrintProvider,
    private val loadFilesUseCase: LoadFilesUseCase,
    private val startPrintJobUseCase: StartPrintJobUseCase,
    val picasso: LiveData<Picasso?>
) : BaseViewModel() {

    val rootFilesMediator = MediatorLiveData<List<FileObject>>()
    private var rootFilesInitialised = false

    fun loadRootFiles(): LiveData<List<FileObject>> {
        if (!rootFilesInitialised) {
            rootFilesInitialised = true
            rootFilesMediator.addSource(octoPrintProvider.octoPrint) {
                viewModelScope.launch(coroutineExceptionHandler) {
                    it?.let {
                        val root = loadFilesUseCase.execute(Params(it, FileOrigin.Local))
                        rootFilesMediator.postValue(root)
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
