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
    private var psuTurnedOnAt = 0L

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

    private fun updateUiState(): UiState {
        try {
            // Connection
            val connectionResponse = availableSerialConnections.value
            val connectionResult = (connectionResponse as? PollingLiveData.Result.Success)?.result

            // Printer
            val printerState = printerState.value?.stateId ?: Message.EventMessage.PrinterStateChanged.PrinterState.UNKNOWN

            // PSU
            val psuState = psuState.value ?: if (octoPrintRepository.instanceInformation.value?.supportsPsuPlugin == true) {
                Message.PsuControlPluginMessage(false)
            } else {
                null
            }
            val supportsPsuPlugin = psuState != null
            val psuCyclingState = psuCyclingState.value ?: PsuCycledState.NotCycled

            viewModelScope.launch(Dispatchers.IO) {
                Timber.d("-----")
                Timber.d("ConnectionResult: $connectionResult")
                Timber.d("PrinterState: $printerState")
                Timber.d("PsuState: $psuState")
                Timber.d("PsuCycled: $psuCyclingState")
                Timber.d("PsuJustTurnedOn: ${isPsuJustTurnedOn()}")

                Firebase.analytics.setUserProperty("psu_plugin_available", supportsPsuPlugin.toString())

                if (supportsPsuPlugin) {
                    octoPrintRepository.instanceInformation.value?.let {
                        octoPrintRepository.storeOctoprintInstanceInformation(it.copy(supportsPsuPlugin = supportsPsuPlugin))
                    }
                }
            }

            if (connectionResponse != null) {
                return when {
                    isOctoPrintStarting(connectionResponse) ->
                        UiState.OctoPrintStarting

                    isOctoPrintUnavailable(connectionResponse) || connectionResult == null ->
                        UiState.OctoPrintNotAvailable

                    isPsuBeingCycled(psuCyclingState) ->
                        UiState.PrinterPsuCycling

                    isNoPrinterAvailable(connectionResult) ->
                        UiState.WaitingForPrinterToComeOnline(psuState?.isPsuOn)

                    isPrinterOffline(connectionResult, printerState, psuCyclingState) ->
                        UiState.PrinterOffline(supportsPsuPlugin)

                    isPrinterConnecting(printerState) ->
                        UiState.PrinterConnecting

                    else -> {
                        // Printer ready to connect
                        autoConnect(printerState, connectionResult.options)
                        UiState.WaitingForPrinterToComeOnline(psuState?.isPsuOn)
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e)
        }

        return uiStateMediator.value ?: UiState.Unknown
    }

    private fun isPsuBeingCycled(psuCycledState: PsuCycledState) =
        psuCycledState == PsuCycledState.Cycling

    private fun isOctoPrintUnavailable(connectionResponse: PollingLiveData.Result<ConnectionResponse?>) =
        connectionResponse is PollingLiveData.Result.Failure

    private fun isOctoPrintStarting(connectionResponse: PollingLiveData.Result<ConnectionResponse?>) =
        (connectionResponse as? PollingLiveData.Result.Failure)?.exception is OctoPrintBootingException

    private fun isNoPrinterAvailable(connectionResponse: ConnectionResponse) =
        connectionResponse.options.ports.isEmpty()

    private fun isPrinterOffline(
        connectionResponse: ConnectionResponse,
        printerState: Message.EventMessage.PrinterStateChanged.PrinterState,
        psuState: PsuCycledState
    ) = connectionResponse.options.ports.isNotEmpty() &&
            !isPrinterConnecting(printerState) &&
            !didJustAttemptToConnect() &&
            psuState != PsuCycledState.Cycled

    private fun isPrinterConnecting(printerState: Message.EventMessage.PrinterStateChanged.PrinterState) = listOf(
        Message.EventMessage.PrinterStateChanged.PrinterState.OPERATIONAL,
        Message.EventMessage.PrinterStateChanged.PrinterState.CONNECTING,
        Message.EventMessage.PrinterStateChanged.PrinterState.OPEN_SERIAL
    ).contains(printerState)

    private fun autoConnect(printerState: Message.EventMessage.PrinterStateChanged.PrinterState, connectionOptions: ConnectionResponse.ConnectionOptions) =
        viewModelScope.launch(Dispatchers.IO) {
            octoPrintProvider.octoPrint.value?.let { octoPrint ->
                if (connectionOptions.ports.isNotEmpty() && !didJustAttemptToConnect() && !isPrinterConnecting(printerState)) {
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

    private fun resetConnectionAttempt() {
        lastConnectionAttempt = 0
    }

    private fun isPsuJustTurnedOn() = (System.currentTimeMillis() - psuTurnedOnAt) < 10000

    fun togglePsu() = viewModelScope.launch(coroutineExceptionHandler) {
        octoPrintProvider.octoPrint.value?.let {
            if (psuState.value?.isPsuOn == true) {
                turnOffPsuUseCase.execute(it)
            } else {
                turnOnPsuUseCase.execute(it)
                psuCyclingState.postValue(PsuCycledState.Cycled)
                resetConnectionAttempt()
            }
        }
    }

    fun cyclePsu() = viewModelScope.launch(coroutineExceptionHandler) {
        octoPrintProvider.octoPrint.value?.let {
            psuCyclingState.postValue(PsuCycledState.Cycling)
            cyclePsuUseCase.execute(it)
            psuCyclingState.postValue(PsuCycledState.Cycled)
            resetConnectionAttempt()
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

        object OctoPrintStarting : UiState()
        object OctoPrintNotAvailable : UiState()

        data class WaitingForPrinterToComeOnline(val psuIsOn: Boolean?) : UiState()
        object PrinterConnecting : UiState()
        data class PrinterOffline(val psuSupported: Boolean) : UiState()
        object PrinterPsuCycling : UiState()

        object Unknown : UiState()

    }
}