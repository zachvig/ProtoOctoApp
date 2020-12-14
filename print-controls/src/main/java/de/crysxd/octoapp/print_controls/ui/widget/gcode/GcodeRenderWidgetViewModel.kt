package de.crysxd.octoapp.print_controls.ui.widget.gcode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.datasource.GcodeFileDataSource
import de.crysxd.octoapp.base.gcode.render.models.RenderStyle
import de.crysxd.octoapp.base.repository.GcodeFileRepository
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.base.usecase.GenerateRenderStyleUseCase
import de.crysxd.octoapp.base.usecase.GetCurrentPrinterProfileUseCase
import de.crysxd.octoapp.octoprint.models.files.FileObject
import de.crysxd.octoapp.octoprint.models.profiles.PrinterProfiles
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

@Suppress("EXPERIMENTAL_API_USAGE")
class GcodeRenderWidgetViewModel(
    octoPrintProvider: OctoPrintProvider,
    octoPrintRepository: OctoPrintRepository,
    generateRenderStyleUseCase: GenerateRenderStyleUseCase,
    getCurrentPrinterProfileUseCase: GetCurrentPrinterProfileUseCase,
    private val gcodeFileRepository: GcodeFileRepository
) : ViewModel() {

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
    }.combine(octoPrintRepository.instanceInformationFlow()) { i12, info ->
        val style = generateRenderStyleUseCase.execute(info)
        Triple(style, i12.first, i12.second)
    }.combine(printInfo) { i123, info ->
        RenderData(i123.first, i123.second, i123.third, info)
    }.shareIn(viewModelScope, SharingStarted.Lazily).catch {
        Timber.e(it)
    }.distinctUntilChanged()

    init {
        viewModelScope.launch {
            printerProfileChannel.offer(getCurrentPrinterProfileUseCase.execute(Unit))
        }
    }

    fun downloadGcode(file: FileObject.File, allowLargeFileDownloads: Boolean) = viewModelScope.launch {
        downloadChannel.offer(gcodeFileRepository.loadFile(file, allowLargeFileDownloads))
    }

    data class PrintInfo(
        val file: FileObject.File,
        val printedBytes: Long,
    )

    data class RenderData(
        val renderStyle: RenderStyle? = null,
        val gcode: GcodeFileDataSource.LoadState,
        val printerProfile: PrinterProfiles.Profile? = null,
        val printInfo: PrintInfo? = null
    )
}