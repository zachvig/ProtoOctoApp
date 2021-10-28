package de.crysxd.octoapp.filemanager.ui.select_file

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import de.crysxd.octoapp.octoprint.models.files.FileObject

class MoveAndCopyFilesViewModel : ViewModel() {
    val selectedFile = MutableLiveData<FileObject?>(null)
    var copyFile = false
}