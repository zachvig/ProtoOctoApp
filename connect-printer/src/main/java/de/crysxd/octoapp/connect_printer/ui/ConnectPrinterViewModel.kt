package de.crysxd.octoapp.connect_printer.ui

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewModelScope
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.livedata.OctoTransformations.filter
import de.crysxd.octoapp.base.livedata.OctoTransformations.filterEventsForMessageType
import de.crysxd.octoapp.base.livedata.PollingLiveData
import de.crysxd.octoapp.base.ui.BaseViewModel
import de.crysxd.octoapp.base.usecase.AutoConnectPrinterUseCase
import de.crysxd.octoapp.base.usecase.GetPrinterConnectionUseCase
import de.crysxd.octoapp.base.usecase.TurnOnPsuUseCase
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
    private val autoConnectPrinterUseCase: AutoConnectPrinterUseCase,
    private val getPrinterConnectionUseCase: GetPrinterConnectionUseCase
) : BaseViewModel() {

    private var lastConnectionAttempt = 0L

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

    private val uiStateMediator = MediatorLiveData<UiState>()
    val uiState = Transformations.map(uiStateMediator) { it }

    init {
        uiStateMediator.addSource(availableSerialConnections) { uiStateMediator.postValue(updateUiState()) }
        uiStateMediator.addSource(printerState) { uiStateMediator.postValue(updateUiState()) }
    }

    private fun updateUiState(): UiState {
        val connectionResult = availableSerialConnections.value
        val printerState = printerState.value as? Message.EventMessage.PrinterStateChanged

        Timber.d("-----")
        Timber.d(connectionResult.toString())
        Timber.d(printerState.toString())

        return when {
            connectionResult is PollingLiveData.Result.Failure -> when (connectionResult.exception) {
                is OctoPrintBootingException -> UiState.OctoPrintStarting
                else -> UiState.OctoPrintNotAvailable
            }

            isConnecting(printerState?.stateId) -> {
                UiState.PrinterConnecting
            }

            connectionResult is PollingLiveData.Result.Success -> when (connectionResult.result?.options) {
                null -> UiState.OctoPrintNotAvailable
                else -> {
                    autoConnect(printerState?.stateId, connectionResult.result!!.options)
                    UiState.WaitingForPrinterToComeOnline
                }
            }

            else -> UiState.Unknown
        }
    }

    private fun autoConnect(printerState: Message.EventMessage.PrinterStateChanged.PrinterState?, connectionOptions: ConnectionResponse.ConnectionOptions) =
        viewModelScope.launch(Dispatchers.IO) {
            octoPrintProvider.octoPrint.value?.let { octoPrint ->
                if (connectionOptions.ports.isNotEmpty() && !didJustAttemptToConnect() && !isConnecting(printerState)) {
                    recordConnectionAttemp()
                    Timber.i("Attempting auto connect")
                    autoConnectPrinterUseCase.execute(octoPrint)
                }
            }
        }

    private fun didJustAttemptToConnect() =
        (System.nanoTime() - lastConnectionAttempt) < TimeUnit.SECONDS.toNanos(10)

    private fun recordConnectionAttemp() {
        lastConnectionAttempt = System.nanoTime()
    }

    private fun isConnecting(printerState: Message.EventMessage.PrinterStateChanged.PrinterState?) = listOf(
        Message.EventMessage.PrinterStateChanged.PrinterState.OPERATIONAL,
        Message.EventMessage.PrinterStateChanged.PrinterState.CONNECTING,
        Message.EventMessage.PrinterStateChanged.PrinterState.DETECT_SERIAL,
        Message.EventMessage.PrinterStateChanged.PrinterState.OPEN_SERIAL
    ).contains(printerState)

    fun turnOnPsu() = viewModelScope.launch(coroutineExceptionHandler) {
        octoPrintProvider.octoPrint.value?.let {
            turnOnPsuUseCase.execute(it)
        }
    }

    sealed class UiState {

        object OctoPrintNotAvailable : UiState()
        object OctoPrintStarting : UiState()
        object WaitingForPrinterToComeOnline : UiState()
        object PrinterConnecting : UiState()
        object Unknown : UiState()

    }
}