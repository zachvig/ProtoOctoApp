package de.crysxd.octoapp.pre_print_controls.ui.file_details

import androidx.lifecycle.viewModelScope
import de.crysxd.octoapp.base.datasource.GcodeFileDataSource
import de.crysxd.octoapp.base.repository.GcodeFileRepository
import de.crysxd.octoapp.base.ui.BaseViewModel
import de.crysxd.octoapp.base.usecase.StartPrintJobUseCase
import de.crysxd.octoapp.octoprint.models.files.FileObject
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@Suppress("EXPERIMENTAL_API_USAGE")
class FileDetailsViewModel(
    private val startPrintJobUseCase: StartPrintJobUseCase,
    private val gcodeFileRepository: GcodeFileRepository
) : BaseViewModel() {

    lateinit var file: FileObject.File
    private val downloadChannel = ConflatedBroadcastChannel<Flow<GcodeFileDataSource.LoadState>?>()
    val gcodeDownloadFlow = downloadChannel.asFlow().filterNotNull().flatMapLatest { it }

    fun startPrint(file: FileObject.File) = viewModelScope.launch(coroutineExceptionHandler) {
        startPrintJobUseCase.execute(file)
    }

    fun downloadGcode() = viewModelScope.launch {
        if (downloadChannel.valueOrNull == null) {
            val flow = gcodeFileRepository.loadFile(file).onCompletion {
                downloadChannel.offer(null)
            }

            downloadChannel.offer(flow)
        }
    }
}
