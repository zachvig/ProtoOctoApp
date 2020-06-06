package de.crysxd.octoapp.pre_print_controls.ui.select_file

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewModelScope
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.ui.BaseViewModel
import de.crysxd.octoapp.octoprint.models.files.FileList
import kotlinx.coroutines.launch

class SelectFileViewModel(
    octoPrintProvider: OctoPrintProvider
) : BaseViewModel() {

    private val filesMediator = MediatorLiveData<FileList>()
    val files = Transformations.map(filesMediator) { it }

    init {
        filesMediator.addSource(octoPrintProvider.octoPrint) {
            viewModelScope.launch(coroutineExceptionHandler) {
                it?.let {
                    filesMediator.postValue(LoadFilesUseCase().execute(it))
                }
            }
        }
    }
}
