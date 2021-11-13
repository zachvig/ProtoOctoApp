package de.crysxd.octoapp.printcontrols.ui.widget.progress

import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import de.crysxd.baseui.BaseViewModel
import de.crysxd.octoapp.base.ext.rateLimit
import de.crysxd.octoapp.base.network.OctoPrintProvider
import de.crysxd.octoapp.octoprint.models.files.FileObject
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber

class ProgressWidgetViewModel(
    private val octoPrintProvider: OctoPrintProvider,
) : BaseViewModel() {

    private var resolvedFile: FileObject.File? = null
    private var resolvingFile = false

    val printState = octoPrintProvider.passiveCurrentMessageFlow("progress_widget")
        .filter { it.progress != null }
        .rateLimit(2000)
        .map { msg ->
            val file = msg.job?.file
            val resolvedFile = resolvedFile?.takeIf { it.path == file?.path } ?: file?.also {
                if (!resolvingFile) {
                    resolveFile(file)
                }
            }

            resolvedFile?.let { msg.copy(job = msg.job?.copy(it)) } ?: msg
        }.asLiveData()

    private fun resolveFile(fileObject: FileObject) = viewModelScope.launch {
        try {
            resolvingFile = true
            Timber.i("Resolving file for current job: ${fileObject.path}")
            resolvedFile = octoPrintProvider.octoPrint().createFilesApi().getFile(fileObject.origin, fileObject.path)
            Timber.i("Resolved file for current job: ${fileObject.path} -> thumbnail=${resolvedFile?.thumbnail}")
        } catch (e: Exception) {
            Timber.e(e)
        } finally {
            resolvingFile = false
        }
    }
}