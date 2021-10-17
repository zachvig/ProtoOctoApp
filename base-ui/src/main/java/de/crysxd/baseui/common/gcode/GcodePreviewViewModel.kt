package de.crysxd.baseui.common.gcode

import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import de.crysxd.baseui.BaseViewModel
import de.crysxd.octoapp.base.OctoPreferences
import de.crysxd.octoapp.base.billing.BillingManager
import de.crysxd.octoapp.base.billing.BillingManager.FEATURE_GCODE_PREVIEW
import de.crysxd.octoapp.base.data.models.GcodePreviewSettings
import de.crysxd.octoapp.base.data.repository.GcodeFileRepository
import de.crysxd.octoapp.base.data.repository.OctoPrintRepository
import de.crysxd.octoapp.base.data.source.GcodeFileDataSource
import de.crysxd.octoapp.base.gcode.render.GcodeRenderContextFactory
import de.crysxd.octoapp.base.gcode.render.models.GcodeRenderContext
import de.crysxd.octoapp.base.gcode.render.models.RenderStyle
import de.crysxd.octoapp.base.network.OctoPrintProvider
import de.crysxd.octoapp.base.usecase.GenerateRenderStyleUseCase
import de.crysxd.octoapp.octoprint.models.files.FileObject
import de.crysxd.octoapp.octoprint.models.profiles.PrinterProfiles
import de.crysxd.octoapp.octoprint.models.socket.Message
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

@Suppress("EXPERIMENTAL_API_USAGE")
class GcodePreviewViewModel(
    octoPrintProvider: OctoPrintProvider,
    octoPrintRepository: OctoPrintRepository,
    octoPreferences: OctoPreferences,
    generateRenderStyleUseCase: GenerateRenderStyleUseCase,
    private val gcodeFileRepository: GcodeFileRepository
) : BaseViewModel() {

    init {
        Timber.i("New instance")
    }

    private var filePendingToLoad: FileObject.File? = null
    private val gcodeFlow = MutableStateFlow<Flow<GcodeFileDataSource.LoadState>?>(emptyFlow())
    private val contextFactoryFlow = MutableStateFlow<(Message.CurrentMessage) -> Pair<GcodeRenderContextFactory, Boolean>?> { null }
    private val manualViewStateFlow = MutableStateFlow<ViewState>(ViewState.Loading())
    private val renderStyleFlow = octoPrintRepository.instanceInformationFlow().combine(octoPreferences.updatedFlow) { instance, _ ->
        generateRenderStyleUseCase.execute(instance)
    }

    private val printerProfileFlow = octoPrintRepository.instanceInformationFlow().map {
        it?.activeProfile ?: PrinterProfiles.Profile()
    }

    private val featureEnabledFlow: Flow<Boolean> = BillingManager.billingFlow().map {
        val enabled = isFeatureEnabled()
        Timber.i("Feature enabled: $enabled")
        enabled
    }.onEach {
        if (it) {
            filePendingToLoad?.let { file ->
                filePendingToLoad = null
                downloadGcode(file, true)
            }
        }
    }

    val activeFile = octoPrintProvider.passiveCurrentMessageFlow("gcode_preview_2").mapNotNull {
        it.job?.file ?: return@mapNotNull null
    }.distinctUntilChangedBy { it.path }.asLiveData()

    private val renderContextFlow: Flow<ViewState> = gcodeFlow.filterNotNull().flatMapLatest { it }
        .combine(octoPrintProvider.passiveCurrentMessageFlow("gcode_preview_1").sample(1000)) { gcodeState, currentMessage ->
            Pair(gcodeState, currentMessage)
        }.combine(contextFactoryFlow) { pair, factory ->
            val (gcodeState, currentMessage) = pair
            val settings = octoPreferences.gcodePreviewSettings

            when (gcodeState) {
                is GcodeFileDataSource.LoadState.Loading -> ViewState.Loading(gcodeState.progress)
                GcodeFileDataSource.LoadState.FailedLargeFileDownloadRequired -> ViewState.LargeFileDownloadRequired
                is GcodeFileDataSource.LoadState.Failed -> ViewState.Error(gcodeState.exception)
                is GcodeFileDataSource.LoadState.Ready -> {
                    factory(currentMessage)?.let {
                        val (factoryInstance, fromUser) = it
                        ViewState.DataReady(
                            renderContext = factoryInstance.extractMoves(
                                gcode = gcodeState.gcode,
                                includePreviousLayer = settings.showPreviousLayer,
                                includeRemainingCurrentLayer = settings.showCurrentLayer,
                            ),
                            fromUser = fromUser,
                            settings = settings
                        )
                    } ?: ViewState.Loading(1f)
                }
            }
        }.distinctUntilChanged()

    private val internalViewState: Flow<ViewState> =
        combine(featureEnabledFlow, renderContextFlow, renderStyleFlow, printerProfileFlow) { featureEnabled, s1, renderStyle, printerProfile ->
            when {
                !featureEnabled -> ViewState.FeatureDisabled(renderStyle)
                s1 is ViewState.DataReady -> s1.copy(renderStyle = renderStyle, printerProfile = printerProfile)
                else -> s1
            }
        }.retry(3) {
            Timber.e(it)
            delay(500L)
            true
        }.catch { e ->
            emit(ViewState.Error(e))
        }

    val viewState = merge(manualViewStateFlow, internalViewState)
        .asLiveData(viewModelScope.coroutineContext)


    private fun isFeatureEnabled() = BillingManager.isFeatureEnabled(FEATURE_GCODE_PREVIEW)

    fun downloadGcode(file: FileObject.File, allowLargeFileDownloads: Boolean) = viewModelScope.launch(coroutineExceptionHandler) {
        Timber.i("Download file: ${file.path}")
        gcodeFlow.value = flowOf(GcodeFileDataSource.LoadState.Loading(0f))

        if (isFeatureEnabled()) {
            gcodeFlow.value = gcodeFileRepository.loadFile(file, allowLargeFileDownloads)
        } else {
            // Feature currently disabled. Store the file to be loaded once the feature got enabled.
            filePendingToLoad = file
        }
    }

    fun useLiveProgress() {
        contextFactoryFlow.value = {
            Pair(
                GcodeRenderContextFactory.ForFileLocation(it.progress?.filepos?.toInt() ?: Int.MAX_VALUE),
                false
            )
        }
    }

    fun useManualProgress(layer: Int, progress: Float) {
        contextFactoryFlow.value = {
            Pair(
                GcodeRenderContextFactory.ForLayerProgress(layerIndex = layer, progress = progress),
                true
            )
        }
    }

    sealed class ViewState {
        data class Loading(val progress: Float = 0f) : ViewState()
        data class FeatureDisabled(val renderStyle: RenderStyle) : ViewState()
        object LargeFileDownloadRequired : ViewState()
        data class Error(val exception: Throwable) : ViewState()
        data class DataReady(
            val renderStyle: RenderStyle? = null,
            val renderContext: GcodeRenderContext? = null,
            val printerProfile: PrinterProfiles.Profile? = null,
            val fromUser: Boolean? = null,
            val settings: GcodePreviewSettings,
        ) : ViewState()
    }
}