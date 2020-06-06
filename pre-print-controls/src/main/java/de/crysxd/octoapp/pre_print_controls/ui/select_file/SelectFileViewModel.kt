package de.crysxd.octoapp.pre_print_controls.ui.select_file

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewModelScope
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.ui.BaseViewModel
import de.crysxd.octoapp.octoprint.models.files.FileObject
import de.crysxd.octoapp.octoprint.models.files.FileOrigin
import kotlinx.coroutines.launch

class SelectFileViewModel(
    octoPrintProvider: OctoPrintProvider
) : BaseViewModel() {

    private val filesMediator = MediatorLiveData<List<FileObject>>()
    val files = Transformations.map(filesMediator) { it }

    init {
        filesMediator.addSource(octoPrintProvider.octoPrint) {
            viewModelScope.launch(coroutineExceptionHandler) {
                it?.let {
                    val root = LoadFilesUseCase().execute(Triple(it, FileOrigin.Local, null))
                    filesMediator.postValue(root)
                    val test = LoadFilesUseCase().execute(Triple(it, FileOrigin.Local, root.first { it is FileObject.Folder } as FileObject.Folder))
                    filesMediator.postValue(test)
                }
            }
        }
    }
}
