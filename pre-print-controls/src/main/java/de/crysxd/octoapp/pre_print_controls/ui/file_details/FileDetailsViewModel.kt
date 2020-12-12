package de.crysxd.octoapp.pre_print_controls.ui.file_details

import androidx.lifecycle.viewModelScope
import de.crysxd.octoapp.base.datasource.GcodeFileDataSource
import de.crysxd.octoapp.base.gcode.render.models.RenderStyle
import de.crysxd.octoapp.base.repository.GcodeFileRepository
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.base.ui.BaseViewModel
import de.crysxd.octoapp.base.usecase.GenerateRenderStyleUseCase
import de.crysxd.octoapp.base.usecase.GetCurrentPrinterProfileUseCase
import de.crysxd.octoapp.base.usecase.StartPrintJobUseCase
import de.crysxd.octoapp.octoprint.models.files.FileObject
import de.crysxd.octoapp.octoprint.models.profiles.PrinterProfiles
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@Suppress("EXPERIMENTAL_API_USAGE")
class FileDetailsViewModel(
    private val octoPrintRepository: OctoPrintRepository,
    private val getCurrentPrinterProfileUseCase: GetCurrentPrinterProfileUseCase,
    private val generateRenderStyleUseCase: GenerateRenderStyleUseCase,
    private val startPrintJobUseCase: StartPrintJobUseCase,
    private val gcodeFileRepository: GcodeFileRepository
) : BaseViewModel() {

    lateinit var file: FileObject.File
    private val renderStyleChannel = ConflatedBroadcastChannel<RenderStyle>(GenerateRenderStyleUseCase.defaultStyle)
    private val profileChannel = ConflatedBroadcastChannel<PrinterProfiles.Profile?>(null)
    private val downloadChannel = ConflatedBroadcastChannel<Flow<GcodeFileDataSource.LoadState>?>()
    val gcodeDownloadFlow = downloadChannel.asFlow().filterNotNull()
        .flatMapLatest {
            it
        }.combine(profileChannel.asFlow()) { download, profile ->
            Pair(download, profile)
        }.combine(octoPrintRepository.instanceInformationFlow()) { pair, info ->
            val style = generateRenderStyleUseCase.execute(info)
            Triple(pair.first, pair.second, style)
        }

    init {
        viewModelScope.launch {
            profileChannel.offer(getCurrentPrinterProfileUseCase.execute(Unit))
        }
    }

    fun startPrint() = viewModelScope.launch(coroutineExceptionHandler) {
        startPrintJobUseCase.execute(file)
    }

    fun downloadGcode(allowLargeFileDownloads: Boolean) = viewModelScope.launch {
        if (downloadChannel.valueOrNull == null) {
            val flow = gcodeFileRepository.loadFile(file, allowLargeFileDownloads).onCompletion {
                downloadChannel.offer(null)
            }

            downloadChannel.offer(flow)
        }
    }
}
