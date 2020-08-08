package de.crysxd.octoapp.connect_printer.ui

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.*
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.livedata.OctoTransformations.filterEventsForMessageType
import de.crysxd.octoapp.base.livedata.OctoTransformations.map
import de.crysxd.octoapp.base.livedata.PollingLiveData
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.base.ui.BaseViewModel
import de.crysxd.octoapp.base.usecase.*
import de.crysxd.octoapp.base.usecase.AutoConnectPrinterUseCase.Params
import de.crysxd.octoapp.connect_printer.R
import de.crysxd.octoapp.octoprint.exceptions.OctoPrintBootingException
import de.crysxd.octoapp.octoprint.models.connection.ConnectionResponse
import de.crysxd.octoapp.octoprint.models.connection.ConnectionResponse.ConnectionState.ERROR_FAILED_TO_AUTODETECT_SERIAL_PORT
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
    private val octoPrintRepository: OctoPrintRepository,
    private val openOctoprintWebUseCase: OpenOctoprintWebUseCase
) : BaseViewModel() {

    private val connectionTimeoutNs = TimeUnit.SECONDS.toNanos(Firebase.remoteConfig.getLong("printer_connection_timeout_sec"))
    private var lastConnectionAttempt = 0L
    private var psuCyclingState = MutableLiveData<PsuCycledState>(PsuCycledState.NotCycled)

    private val availableSerialConnections = Transformations.switchMap(octoPrintProvider.octoPrint) {
        PollingLiveData {
            it?.let {
                getPrinterConnectionUseCase.execute(it)
            }
        }
    }

    private val psuState = octoPrintProvider.eventLiveData
        .filterEventsForMessageType(Message.PsuControlPluginMessage::class.java)

    private val uiStateMediator = MediatorLiveData<UiState>()
    val uiState = uiStateMediator.map { it }.distinctUntilChanged()

    init {
        uiStateMediator.addSource(availableSerialConnections) { uiStateMediator.postValue(updateUiState()) }
        uiStateMediator.addSource(psuState) { uiStateMediator.postValue(updateUiState()) }
        uiStateMediator.addSource(psuCyclingState) { uiStateMediator.postValue(updateUiState()) }
        uiStateMediator.value = UiState.Initializing
    }

    private fun updateUiState(): UiState {
        try {
            // Connection
            val connectionResponse = availableSerialConnections.value
            val connectionResult = (connectionResponse as? PollingLiveData.Result.Success)?.result

            // PSU
            val psuState = psuState.value ?: if (octoPrintRepository.instanceInformation.value?.supportsPsuPlugin == true) {
                Message.PsuControlPluginMessage(false)
            } else {
                null
            }
            val supportsPsuPlugin = psuState != null
            val psuCyclingState = psuCyclingState.value ?: PsuCycledState.NotCycled

            viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler) {
                Timber.d("-----")
                Timber.d("ConnectionResult: $connectionResult")
                Timber.d("PsuState: $psuState")
                Timber.d("PsuCycled: $psuCyclingState")

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

                    isPrinterConnected(connectionResult) ->
                        UiState.PrinterConnected

                    isNoPrinterAvailable(connectionResult) ->
                        UiState.WaitingForPrinterToComeOnline(psuState?.isPsuOn)

                    isPrinterOffline(connectionResult, psuCyclingState) ->
                        UiState.PrinterOffline(supportsPsuPlugin)

                    isPrinterConnecting(connectionResult) ->
                        UiState.PrinterConnecting

                    else -> {
                        // Printer ready to connect
                        autoConnect(connectionResult)
                        UiState.WaitingForPrinterToComeOnline(psuState?.isPsuOn)
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e)
        }

        return uiStateMediator.value ?: UiState.Initializing
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
        psuState: PsuCycledState
    ) = connectionResponse.options.ports.isNotEmpty() &&
            (isInErrorState(connectionResponse) || isConnectionAttemptTimedOut(connectionResponse)) &&
            psuState != PsuCycledState.Cycled

    private fun isConnectionAttemptTimedOut(connectionResponse: ConnectionResponse) = isPrinterConnecting(connectionResponse) &&
            System.nanoTime() - lastConnectionAttempt > connectionTimeoutNs

    private fun isInErrorState(connectionResponse: ConnectionResponse) = listOf(
        ConnectionResponse.ConnectionState.UNKNOWN_ERROR,
        ConnectionResponse.ConnectionState.CONNECTION_ERROR
    ).contains(connectionResponse.current.state)

    private fun isPrinterConnecting(connectionResponse: ConnectionResponse) = listOf(
        ConnectionResponse.ConnectionState.CONNECTING,
        ConnectionResponse.ConnectionState.DETECTING_SERIAL_PORT,
        ConnectionResponse.ConnectionState.DETECTING_BAUDRATE
    ).contains(connectionResponse.current.state)

    private fun isPrinterConnected(connectionResponse: ConnectionResponse) = listOf(
        ConnectionResponse.ConnectionState.OPERATIONAL,
        ConnectionResponse.ConnectionState.PRINTING
    ).contains(connectionResponse.current.state)

    private fun autoConnect(connectionResponse: ConnectionResponse) = viewModelScope.launch(coroutineExceptionHandler) {
        octoPrintProvider.octoPrint.value?.let { octoPrint ->
            if (connectionResponse.options.ports.isNotEmpty() && !didJustAttemptToConnect() && !isPrinterConnecting(connectionResponse)) {
                recordConnectionAttempt()
                Timber.i("Attempting auto connect")
                autoConnectPrinterUseCase.execute(
                    if (connectionResponse.current.state == ERROR_FAILED_TO_AUTODETECT_SERIAL_PORT) {
                        // TODO Ask user which port to select
                        val app = Injector.get().app()
                        Toast.makeText(app, app.getString(R.string.auto_selection_failed), Toast.LENGTH_SHORT).show()
                        Firebase.analytics.logEvent("auto_connect_failed", Bundle.EMPTY)
                        Params(octoPrint, connectionResponse.options.ports.first())
                    } else {
                        Params(octoPrint)
                    }
                )
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
        lastConnectionAttempt = 0L
        psuCyclingState.postValue(PsuCycledState.Cycled)
    }

    fun openWebInterface(context: Context) = viewModelScope.launch(coroutineExceptionHandler) {
        octoPrintProvider.octoPrint.value?.let {
            openOctoprintWebUseCase.execute(Pair(it, context))
        }
    }

    private sealed class PsuCycledState {
        object NotCycled : PsuCycledState()
        object Cycled : PsuCycledState()
        object Cycling : PsuCycledState()
    }

    sealed class UiState {

        object Initializing : UiState()

        object OctoPrintStarting : UiState()
        object OctoPrintNotAvailable : UiState()

        data class WaitingForPrinterToComeOnline(val psuIsOn: Boolean?) : UiState()
        object PrinterConnecting : UiState()
        data class PrinterOffline(val psuSupported: Boolean) : UiState()
        object PrinterPsuCycling : UiState()
        object PrinterConnected : UiState()

        object Unknown : UiState()

    }
}