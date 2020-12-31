package de.crysxd.octoapp.base.ui.common.gcode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.billing.BillingManager
import de.crysxd.octoapp.base.datasource.GcodeFileDataSource
import de.crysxd.octoapp.base.gcode.render.GcodeRenderContextFactory
import de.crysxd.octoapp.base.gcode.render.models.GcodeRenderContext
import de.crysxd.octoapp.base.gcode.render.models.RenderStyle
import de.crysxd.octoapp.base.repository.GcodeFileRepository
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.base.usecase.GenerateRenderStyleUseCase
import de.crysxd.octoapp.base.usecase.GetCurrentPrinterProfileUseCase
import de.crysxd.octoapp.octoprint.models.files.FileObject
import de.crysxd.octoapp.octoprint.models.profiles.PrinterProfiles
import de.crysxd.octoapp.octoprint.models.socket.Message
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@Suppress("EXPERIMENTAL_API_USAGE")
class GcodePreviewViewModel(
    octoPrintProvider: OctoPrintProvider,
    octoPrintRepository: OctoPrintRepository,
    generateRenderStyleUseCase: GenerateRenderStyleUseCase,
    getCurrentPrinterProfileUseCase: GetCurrentPrinterProfileUseCase,
    private val gcodeFileRepository: GcodeFileRepository
) : ViewModel() {

    private val gcodeChannel = ConflatedBroadcastChannel<Flow<GcodeFileDataSource.LoadState>?>()
    private val contextFactoryChannel = ConflatedBroadcastChannel<(Message.CurrentMessage) -> Pair<GcodeRenderContextFactory, Boolean>>()
    private val manualViewStateChannel = ConflatedBroadcastChannel<ViewState>(ViewState.Loading())
    private val renderStyleFlow: Flow<ViewState> = octoPrintRepository.instanceInformationFlow().map {
        ViewState.DataReady(renderStyle = generateRenderStyleUseCase.execute(it))
    }

    private val printerProfileFlow: Flow<ViewState> = flow {
        emit(ViewState.DataReady(printerProfile = getCurrentPrinterProfileUseCase.execute(Unit)))
    }

    private val renderContextFlow: Flow<ViewState> = gcodeChannel.asFlow().filterNotNull().flatMapLatest { it }
        .combine(octoPrintProvider.passiveCurrentMessageFlow()) { gcodeState, currentMessage ->
            Pair(gcodeState, currentMessage)
        }.combine(contextFactoryChannel.asFlow()) { pair, factory ->
            val (gcodeState, currentMessage) = pair

            when (gcodeState) {
                is GcodeFileDataSource.LoadState.Loading -> ViewState.Loading(gcodeState.progress)
                GcodeFileDataSource.LoadState.FailedLargeFileDownloadRequired -> ViewState.LargeFileDownloadRequired
                is GcodeFileDataSource.LoadState.Failed -> ViewState.Error(gcodeState.exception)
                is GcodeFileDataSource.LoadState.Ready -> {
                    val (factoryInstance, fromUser) = factory(currentMessage)
                    ViewState.DataReady(
                        renderContext = factoryInstance.extractMoves(gcodeState.gcode),
                        fromUser = fromUser
                    )
                }
            }
        }.retry(3) {
            delay(500L)
            true
        }.catch {
            emit(ViewState.Error(it))
        }.distinctUntilChanged()

    private val internalViewState: Flow<ViewState> =
        combine(BillingManager.billingFlow(), renderContextFlow, renderStyleFlow, printerProfileFlow) { billing, s1, s2, s3 ->
            val all = listOf(s1, s2, s3)
            val error = all.mapNotNull { it as? ViewState.Error }.firstOrNull()
            val loadingProgress = all.mapNotNull { it as? ViewState.Loading }.minByOrNull { it.progress }

            when {
                !billing.isPremiumActive -> ViewState.FeatureDisabled
                error != null -> error
                all.any { it is ViewState.LargeFileDownloadRequired } -> ViewState.LargeFileDownloadRequired
                loadingProgress != null -> ViewState.Loading(loadingProgress.progress)
                else /* All success*/ -> all.map { it as ViewState.DataReady }.reduce { o1, o2 ->
                    ViewState.DataReady(
                        renderStyle = o1.renderStyle ?: o2.renderStyle,
                        renderContext = o1.renderContext ?: o2.renderContext,
                        printerProfile = o1.printerProfile ?: o2.printerProfile,
                        fromUser = o1.fromUser ?: o2.fromUser,
                    )
                }
            }
        }.retry(3) {
            delay(500L)
            true
        }.catch { e ->
            emit(ViewState.Error(e))
        }

    val viewState = merge(manualViewStateChannel.asFlow(), internalViewState)
        .asLiveData(viewModelScope.coroutineContext)

    fun downloadGcode(file: FileObject.File, allowLargeFileDownloads: Boolean) = viewModelScope.launch {
        manualViewStateChannel.offer(ViewState.Loading())
        gcodeChannel.offer(gcodeFileRepository.loadFile(file, allowLargeFileDownloads))
    }

    fun useLiveProgress() {
        contextFactoryChannel.offer {
            Pair(
                GcodeRenderContextFactory.ForFileLocation(it.progress?.filepos?.toInt() ?: Int.MAX_VALUE),
                false
            )
        }
    }

    fun useManualProgress(layer: Int, progress: Float) {
        contextFactoryChannel.offer {
            Pair(
                GcodeRenderContextFactory.ForLayerProgress(layer = layer, progress = progress),
                true
            )
        }
    }

    sealed class ViewState {
        data class Loading(val progress: Float = 0f) : ViewState()
        object FeatureDisabled : ViewState()
        object LargeFileDownloadRequired : ViewState()
        data class Error(val exception: Throwable) : ViewState()
        data class DataReady(
            val renderStyle: RenderStyle? = null,
            val renderContext: GcodeRenderContext? = null,
            val printerProfile: PrinterProfiles.Profile? = null,
            val fromUser: Boolean? = null
        ) : ViewState()
    }
}