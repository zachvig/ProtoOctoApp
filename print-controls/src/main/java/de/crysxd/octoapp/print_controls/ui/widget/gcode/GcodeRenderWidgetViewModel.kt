package de.crysxd.octoapp.print_controls.ui.widget.gcode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.datasource.GcodeFileDataSource
import de.crysxd.octoapp.base.gcode.render.models.RenderStyle
import de.crysxd.octoapp.base.repository.GcodeFileRepository
import de.crysxd.octoapp.base.usecase.GenerateRenderStyleUseCase
import de.crysxd.octoapp.base.usecase.GetCurrentPrinterProfileUseCase
import de.crysxd.octoapp.octoprint.models.files.FileObject
import de.crysxd.octoapp.octoprint.models.profiles.PrinterProfiles
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@Suppress("EXPERIMENTAL_API_USAGE")
class GcodeRenderWidgetViewModel(
    octoPrintProvider: OctoPrintProvider,
    generateRenderStyleUseCase: GenerateRenderStyleUseCase,
    getCurrentPrinterProfileUseCase: GetCurrentPrinterProfileUseCase,
    private val gcodeFileRepository: GcodeFileRepository
) : ViewModel() {

    private var renderStyleChannel = ConflatedBroadcastChannel<RenderStyle>()
    private val downloadChannel = ConflatedBroadcastChannel<Flow<GcodeFileDataSource.LoadState>?>()
    private val printerProfileChannel = ConflatedBroadcastChannel<PrinterProfiles.Profile>()

    private val printInfo = octoPrintProvider.passiveCurrentMessageFlow().map {
        val file = it.job?.file ?: return@map null
        val printedBytes = it.progress?.filepos ?: return@map null
        PrintInfo(file, printedBytes)
    }.filterNotNull()

    val file = printInfo.map {
        it.file
    }.distinctUntilChanged { old, new ->
        old.path != new.path
    }

    val renderData = downloadChannel.asFlow().filterNotNull().flatMapLatest {
        it
    }.combine(printerProfileChannel.asFlow()) { i1, i2 ->
        Pair(i1, i2)
    }.combine(renderStyleChannel.asFlow()) { i12, style ->
        Triple(style, i12.first, i12.second)
    }.combine(printInfo) { i123, info ->
        RenderData(i123.first, i123.second, i123.third, info)
    }

    init {
        viewModelScope.launch {
            renderStyleChannel.offer(GenerateRenderStyleUseCase.defaultStyle)
        }
        viewModelScope.launch {
            printerProfileChannel.offer(getCurrentPrinterProfileUseCase.execute(Unit))
        }
    }

    fun downloadGcode(file: FileObject.File, allowLargeFileDownloads: Boolean) = viewModelScope.launch {
        if (downloadChannel.valueOrNull == null) {
            val flow = gcodeFileRepository.loadFile(file, allowLargeFileDownloads).onCompletion {
                downloadChannel.offer(null)
            }
            downloadChannel.offer(flow)
        }
    }

    data class PrintInfo(
        val file: FileObject.File,
        val printedBytes: Long,
    )

    data class RenderData(
        val renderStyle: RenderStyle,
        val gcode: GcodeFileDataSource.LoadState,
        val printerProfile: PrinterProfiles.Profile,
        val printInfo: PrintInfo
    )
}