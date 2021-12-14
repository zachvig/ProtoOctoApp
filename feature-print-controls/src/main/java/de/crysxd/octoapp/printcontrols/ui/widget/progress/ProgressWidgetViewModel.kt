package de.crysxd.octoapp.printcontrols.ui.widget.progress

import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import de.crysxd.baseui.BaseViewModel
import de.crysxd.octoapp.base.OctoPreferences
import de.crysxd.octoapp.base.data.models.ProgressWidgetSettings
import de.crysxd.octoapp.base.ext.rateLimit
import de.crysxd.octoapp.base.network.OctoPrintProvider
import de.crysxd.octoapp.octoprint.models.files.FileObject
import de.crysxd.octoapp.octoprint.models.socket.Message
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import timber.log.Timber

class ProgressWidgetViewModel(
    private val octoPrintProvider: OctoPrintProvider,
    private val octoPreferences: OctoPreferences,
) : BaseViewModel() {

    private var resolvedFile: FileObject.File? = null
    private var resolvingFile = false
    private val fileLoadedTrigger = MutableStateFlow(0)

    val printState = octoPrintProvider.passiveCurrentMessageFlow("progress_widget")
        .filter { it.progress != null }
        .combine(fileLoadedTrigger) { msg, _ -> msg }
        .combine(octoPreferences.updatedFlow) { msg, _ ->
            val file = msg.job?.file
            val resolvedFile = resolvedFile?.takeIf { it.path == file?.path } ?: file?.also {
                if (!resolvingFile) {
                    resolveFile(file)
                }
            }

            val updateMsg = resolvedFile?.let { msg.copy(job = msg.job?.copy(it)) } ?: msg
            updateMsg to octoPreferences.progressWidgetSettings
        }.combine(octoPrintProvider.passiveCompanionMessageFlow("progress_widget")) { pair, companion ->
            ViewState(
                settings = pair.second,
                currentMessage = pair.first,
                companionMessage = companion
            )
        }.rateLimit(2000).asLiveData()

    private fun resolveFile(fileObject: FileObject) = viewModelScope.launch {
        try {
            val origin = fileObject.origin ?: return@launch
            resolvingFile = true
            Timber.i("Resolving file for current job: ${fileObject.path}")
            resolvedFile = octoPrintProvider.octoPrint().createFilesApi().getFile(origin, requireNotNull(fileObject.path))
            fileLoadedTrigger.value++
            Timber.i("Resolved file for current job: ${fileObject.path} -> thumbnail=${resolvedFile?.thumbnail}")
        } catch (e: Exception) {
            Timber.e(e)
        } finally {
            resolvingFile = false
        }
    }

    data class ViewState(
        val settings: ProgressWidgetSettings,
        val currentMessage: Message.CurrentMessage,
        val companionMessage: Message.CompanionPluginMessage?,
    )
}