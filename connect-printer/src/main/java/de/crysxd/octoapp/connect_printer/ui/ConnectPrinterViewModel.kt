package de.crysxd.octoapp.connect_printer.ui

import androidx.lifecycle.*
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.livedata.OctoTransformations.filter
import de.crysxd.octoapp.base.livedata.OctoTransformations.filterEventsForMessageType
import de.crysxd.octoapp.base.livedata.OctoTransformations.map
import de.crysxd.octoapp.base.livedata.PollingLiveData
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.base.ui.BaseViewModel
import de.crysxd.octoapp.base.usecase.*
import de.crysxd.octoapp.octoprint.exceptions.OctoPrintBootingException
import de.crysxd.octoapp.octoprint.models.connection.ConnectionResponse
import de.crysxd.octoapp.octoprint.models.socket.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit

class ConnectPrinterViewModel(
    private val octoPrintProvider: OctoPrintProvider,
    private val turnOnPsuUseCase: TurnOnPsuUseCase,
    private val turnOffPsuUseCase: TurnOffPsuUseCase,
    private val cyclePsuUseCase: CyclePsuUseCase,
    private val autoConnectPrinterUseCase: AutoConnectPrinterUseCase,
    private val getPrinterConnectionUseCase: GetPrinterConnectionUseCase,
    private val octoPrintRepository: OctoPrintRepository
) : BaseViewModel() {

    private var lastConnectionAttempt = 0L
    private var psuCyclingState = MutableLiveData<PsuCycledState>(PsuCycledState.NotCycled)

    private val availableSerialConnections = Transformations.switchMap(octoPrintProvider.octoPrint) {
        PollingLiveData {
            it?.let {
                getPrinterConnectionUseCase.execute(it)
            }
        }
    }

    private val printerState = octoPrintProvider.eventLiveData
        .filterEventsForMessageType(Message.EventMessage::class.java)
        .filter { it is Message.EventMessage.PrinterStateChanged }
        .map { it as Message.EventMessage.PrinterStateChanged }

    private val psuState = octoPrintProvider.eventLiveData
        .filterEventsForMessageType(Message.PsuControlPluginMessage::class.java)

    private val uiStateMediator = MediatorLiveData<UiState>()
    val uiState = uiStateMediator.map { it }.distinctUntilChanged()

    init {
        uiStateMediator.addSource(availableSerialConnections) { uiStateMediator.postValue(updateUiState()) }
        uiStateMediator.addSource(printerState) { uiStateMediator.postValue(updateUiState()) }
        uiStateMediator.addSource(psuState) { uiStateMediator.postValue(updateUiState()) }
        uiStateMediator.addSource(psuCyclingState) { uiStateMediator.postValue(updateUiState()) }
    }

    private fun updateUiState(): UiState = try {
        val connectionResponse = availableSerialConnections.value
        val connectionResult = (connectionResponse as? PollingLiveData.Result.Success)?.result
        val printerState = printerState.value
        val psuState = psuState.value ?: if (octoPrintRepository.instanceInformation.value?.supportsPsuPlugin == true) {
            Message.PsuControlPluginMessage(false)
        } else {
            null
        }
        val supportsPsuPlugin = psuState != null
        val psuCyclingState = psuCyclingState.value ?: PsuCycledState.NotCycled

        viewModelScope.launch(Dispatchers.IO) {
            Firebase.analytics.setUserProperty("psu_plugin_available", supportsPsuPlugin.toString())

            if (supportsPsuPlugin) {
                octoPrintRepository.instanceInformation.value?.let {
                    octoPrintRepository.storeOctoprintInstanceInformation(it.copy(supportsPsuPlugin = supportsPsuPlugin))
                }
            }
        }

        Timber.d("-----")
        Timber.d("ConnectionResult: $connectionResult")
        Timber.d("PrinterState: $printerState")
        Timber.d("PsuState: $psuState")
        Timber.d("PsuCycled: $psuCyclingState")

        when {
            connectionResponse is PollingLiveData.Result.Failure -> when (connectionResponse.exception) {
                is OctoPrintBootingException -> UiState.OctoPrintStarting
                else -> UiState.OctoPrintNotAvailable
            }

            isConnecting(printerState?.stateId) -> UiState.PrinterConnecting

            psuCyclingState == PsuCycledState.Cycling -> UiState.PrinterPsuCycling

            isOffline(connectionResult, printerState?.stateId) && psuCyclingState != PsuCycledState.Cycled && !didJustAttemptToConnect() && psuState?.isPsuOn != true ->
                UiState.PrinterOffline(supportsPsuPlugin)

            isUnknown(printerState?.stateId) -> UiState.Unknown

            connectionResponse is PollingLiveData.Result.Success -> when (connectionResult?.options) {
                null -> UiState.OctoPrintNotAvailable
                else -> {
                    autoConnect(printerState?.stateId, connectionResult.options)
                    UiState.WaitingForPrinterToComeOnline(psuState?.isPsuOn)
                }
            }

            else -> UiState.Unknown
        }
    } catch (e: Exception) {
        Timber.e(e)
        UiState.Unknown
    }

    private fun autoConnect(printerState: Message.EventMessage.PrinterStateChanged.PrinterState?, connectionOptions: ConnectionResponse.ConnectionOptions) =
        viewModelScope.launch(Dispatchers.IO) {
            octoPrintProvider.octoPrint.value?.let { octoPrint ->
                if (connectionOptions.ports.isNotEmpty() && !didJustAttemptToConnect() && !isConnecting(printerState)) {
                    recordConnectionAttempt()
                    Timber.i("Attempting auto connect")
                    autoConnectPrinterUseCase.execute(octoPrint)
                    psuCyclingState.postValue(PsuCycledState.NotCycled)
                }
            }
        }

    private fun didJustAttemptToConnect() =
        (System.nanoTime() - lastConnectionAttempt) < TimeUnit.SECONDS.toNanos(10)

    private fun recordConnectionAttempt() {
        lastConnectionAttempt = System.nanoTime()
    }

    private fun isConnecting(printerState: Message.EventMessage.PrinterStateChanged.PrinterState?) = listOf(
        Message.EventMessage.PrinterStateChanged.PrinterState.OPERATIONAL,
        Message.EventMessage.PrinterStateChanged.PrinterState.CONNECTING,
        Message.EventMessage.PrinterStateChanged.PrinterState.OPEN_SERIAL
    ).contains(printerState)

    private fun isOffline(
        connectionResponse: ConnectionResponse?,
        printerState: Message.EventMessage.PrinterStateChanged.PrinterState?
    ) = listOf(
        Message.EventMessage.PrinterStateChanged.PrinterState.OFFLINE,
        Message.EventMessage.PrinterStateChanged.PrinterState.ERROR
    ).contains(printerState) || connectionResponse?.current?.state.equals("closed", ignoreCase = true)

    private fun isUnknown(printerState: Message.EventMessage.PrinterStateChanged.PrinterState?) = listOf(
        Message.EventMessage.PrinterStateChanged.PrinterState.UNKNOWN
    ).contains(printerState)

    fun togglePsu() = viewModelScope.launch(coroutineExceptionHandler) {
        octoPrintProvider.octoPrint.value?.let {
            if (psuState.value?.isPsuOn == true) {
                turnOffPsuUseCase.execute(it)
            } else {
                turnOnPsuUseCase.execute(it)
            }
        }
    }

    fun cyclePsu() = viewModelScope.launch(coroutineExceptionHandler) {
        octoPrintProvider.octoPrint.value?.let {
            psuCyclingState.postValue(PsuCycledState.Cycling)
            cyclePsuUseCase.execute(it)
            psuCyclingState.postValue(PsuCycledState.Cycled)

        }
    }

    fun retryConnectionFromOfflineState() {
        psuCyclingState.postValue(PsuCycledState.Cycled)
    }

    private sealed class PsuCycledState {
        object NotCycled : PsuCycledState()
        object Cycled : PsuCycledState()
        object Cycling : PsuCycledState()
    }

    sealed class UiState {

        object OctoPrintNotAvailable : UiState()
        object OctoPrintStarting : UiState()
        data class WaitingForPrinterToComeOnline(val psuIsOn: Boolean?) : UiState()
        object PrinterConnecting : UiState()
        data class PrinterOffline(val psuSupported: Boolean) : UiState()
        object PrinterPsuCycling : UiState()
        object Unknown : UiState()

    }
}