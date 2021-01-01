package de.crysxd.octoapp.print_controls.ui.widget.gcode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.billing.BillingManager
import de.crysxd.octoapp.base.datasource.GcodeFileDataSource
import de.crysxd.octoapp.base.gcode.render.models.RenderStyle
import de.crysxd.octoapp.base.repository.GcodeFileRepository
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.base.usecase.GenerateRenderStyleUseCase
import de.crysxd.octoapp.base.usecase.GetCurrentPrinterProfileUseCase
import de.crysxd.octoapp.octoprint.models.files.FileObject
import de.crysxd.octoapp.octoprint.models.profiles.PrinterProfiles
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

@Suppress("EXPERIMENTAL_API_USAGE")
class GcodePreviewWidgetViewModel(
    octoPrintProvider: OctoPrintProvider,
    octoPrintRepository: OctoPrintRepository,
    generateRenderStyleUseCase: GenerateRenderStyleUseCase,
    getCurrentPrinterProfileUseCase: GetCurrentPrinterProfileUseCase,
    private val gcodeFileRepository: GcodeFileRepository
) : ViewModel() {

    private val featureEnabledChannel = ConflatedBroadcastChannel<Boolean>()
    private val downloadChannel = ConflatedBroadcastChannel<Flow<GcodeFileDataSource.LoadState>>()
    private val downloadFlow = downloadChannel.asFlow().flatMapLatest { it }
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

    val renderData = combine(
        featureEnabledChannel.asFlow(),
        downloadFlow,
        printerProfileChannel.asFlow(),
        octoPrintRepository.instanceInformationFlow(),
        printInfo
    ) { enabled, download, profile, instanceInfo, printInfo ->
        val style = generateRenderStyleUseCase.execute(instanceInfo)
        when {
            !enabled -> RenderData(renderStyle = style, featureEnabled = false)
            else -> RenderData(
                featureEnabled = true,
                renderStyle = style,
                gcode = download,
                printerProfile = profile,
                printInfo = printInfo
            )
        }
    }.distinctUntilChanged().retry(3) {
        delay(500L)
        true
    }.catch {
        emit(RenderData(gcode = GcodeFileDataSource.LoadState.Failed(it), featureEnabled = true))
        Timber.e(it)
    }.asLiveData()

    init {
        viewModelScope.launch {
            printerProfileChannel.offer(getCurrentPrinterProfileUseCase.execute(Unit))
        }
    }

    fun downloadGcode(file: FileObject.File, allowLargeFileDownloads: Boolean) = viewModelScope.launch {
        downloadChannel.offer(flowOf(GcodeFileDataSource.LoadState.Loading(0f)))

        if (BillingManager.isFeatureEnabled("gcode_preview")) {
            featureEnabledChannel.offer(true)
            downloadChannel.offer(gcodeFileRepository.loadFile(file, allowLargeFileDownloads))
        } else {
            featureEnabledChannel.offer(false)
        }
    }

    data class PrintInfo(
        val file: FileObject.File,
        val printedBytes: Long,
    )

    data class RenderData(
        val featureEnabled: Boolean,
        val renderStyle: RenderStyle? = null,
        val gcode: GcodeFileDataSource.LoadState? = null,
        val printerProfile: PrinterProfiles.Profile? = null,
        val printInfo: PrintInfo? = null
    )
}