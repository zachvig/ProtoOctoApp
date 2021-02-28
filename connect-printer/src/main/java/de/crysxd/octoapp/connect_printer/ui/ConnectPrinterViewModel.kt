package de.crysxd.octoapp.connect_printer.ui

import android.widget.Toast
import androidx.lifecycle.*
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.base.OctoPreferences
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.livedata.OctoTransformations.map
import de.crysxd.octoapp.base.livedata.PollingLiveData
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.base.ui.base.BaseViewModel
import de.crysxd.octoapp.base.ui.menu.power.PowerControlsMenu
import de.crysxd.octoapp.base.usecase.AutoConnectPrinterUseCase
import de.crysxd.octoapp.base.usecase.AutoConnectPrinterUseCase.Params
import de.crysxd.octoapp.base.usecase.GetPowerDevicesUseCase
import de.crysxd.octoapp.base.usecase.GetPrinterConnectionUseCase
import de.crysxd.octoapp.base.usecase.execute
import de.crysxd.octoapp.connect_printer.R
import de.crysxd.octoapp.octoprint.exceptions.OctoPrintBootingException
import de.crysxd.octoapp.octoprint.models.connection.ConnectionResponse
import de.crysxd.octoapp.octoprint.models.connection.ConnectionResponse.ConnectionState.MAYBE_ERROR_FAILED_TO_AUTODETECT_SERIAL_PORT
import de.crysxd.octoapp.octoprint.plugins.power.PowerDevice
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit


class ConnectPrinterViewModel(
    private val autoConnectPrinterUseCase: AutoConnectPrinterUseCase,
    private val getPrinterConnectionUseCase: GetPrinterConnectionUseCase,
    private val getPowerDevicesUseCase: GetPowerDevicesUseCase,
    private val octoPreferences: OctoPreferences,
    private val octoPrintRepository: OctoPrintRepository,
) : BaseViewModel() {

    private val connectionTimeoutNs = TimeUnit.SECONDS.toNanos(Firebase.remoteConfig.getLong("printer_connection_timeout_sec"))
    private var lastConnectionAttempt = 0L
    private var psuCyclingState = MutableLiveData<PsuCycledState>(PsuCycledState.NotCycled)

    private val availableSerialConnections = PollingLiveData {
        getPrinterConnectionUseCase.execute()
    }

    private var isPsuSupported = false
    private val uiStateMediator = MediatorLiveData<UiState>()
    private val manualPsuState = MutableLiveData<Boolean?>(null)
    private var userAllowedConnectAt = 0L
    private var manualTrigger = MutableLiveData(Unit)
    val uiState = uiStateMediator.map { it }.distinctUntilChanged()
    private val psuPollingLiveData = PollingLiveData(5000) {
        // Check if the default device is turned on or off
        octoPrintRepository.getActiveInstanceSnapshot()?.appSettings?.defaultPowerDevices?.get(PowerControlsMenu.DeviceType.PrinterPsu.prefKey)?.let {
            val state = getPowerDevicesUseCase.execute(GetPowerDevicesUseCase.Params(queryState = true, onlyGetDeviceWithUniqueId = it)).firstOrNull()
            Timber.i("PSU state: $state")
            manualPsuState.postValue(state?.second == GetPowerDevicesUseCase.PowerState.On)
        }
        Unit
    }

    init {
        uiStateMediator.addSource(octoPreferences.updatedFlow.asLiveData()) { updateUiState() }
        uiStateMediator.addSource(availableSerialConnections) { updateUiState() }
        uiStateMediator.addSource(manualPsuState) { updateUiState() }
        uiStateMediator.addSource(psuPollingLiveData) { updateUiState() }
        uiStateMediator.addSource(psuCyclingState) { updateUiState() }
        uiStateMediator.addSource(manualTrigger) { updateUiState() }
        uiStateMediator.value = UiState.Initializing

        viewModelScope.launch {
            isPsuSupported = getPowerDevicesUseCase.execute(GetPowerDevicesUseCase.Params(false)).isNotEmpty()
            computeUiState()
        }
    }

    private fun updateUiState() = viewModelScope.launch(coroutineExceptionHandler) {
        uiStateMediator.postValue(computeUiState())
    }

    private fun computeUiState(): UiState {
        try {
            // Connection
            val connectionResponse = availableSerialConnections.value
            val connectionResult = (connectionResponse as? PollingLiveData.Result.Success)?.result

            // PSU
            val psuCyclingState = psuCyclingState.value ?: PsuCycledState.NotCycled
            val isPsuTurnedOn = if (isPsuSupported) {
                manualPsuState.value
            } else {
                null
            }

            // Are we allowed to automatically connect the printer?
            val isAutoConnect = octoPreferences.isAutoConnectPrinter ||
                    TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - userAllowedConnectAt) < 3

            Timber.d("-----")
            Timber.d("ConnectionResult: $connectionResult")
            Timber.d("PsuSupported: $isPsuSupported")
            Timber.d("PsuCycled: $psuCyclingState")
            Timber.d("PsuState: $isPsuTurnedOn")

            if (connectionResponse != null) {
                return when {
                    isOctoPrintStarting(connectionResponse) ->
                        UiState.OctoPrintStarting

                    isOctoPrintUnavailable(connectionResponse) || connectionResult == null ->
                        UiState.OctoPrintNotAvailable

                    !isAutoConnect ->
                        UiState.WaitingForUser

                    isPsuBeingCycled(psuCyclingState) ->
                        UiState.PrinterPsuCycling

                    isNoPrinterAvailable(connectionResult) ->
                        UiState.WaitingForPrinterToComeOnline(isPsuTurnedOn)

                    isPrinterOffline(connectionResult, psuCyclingState) ->
                        UiState.PrinterOffline(isPsuSupported)

                    isPrinterConnecting(connectionResult) ->
                        UiState.PrinterConnecting

                    isPrinterConnected(connectionResult) ->
                        UiState.PrinterConnected

                    else -> {
                        // Printer ready to connect
                        autoConnect(connectionResult)
                        UiState.WaitingForPrinterToComeOnline(isPsuTurnedOn)
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

    private fun isOctoPrintUnavailable(connectionResponse: PollingLiveData.Result<ConnectionResponse>?) =
        connectionResponse is PollingLiveData.Result.Failure

    private fun isOctoPrintStarting(connectionResponse: PollingLiveData.Result<ConnectionResponse>?) =
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
        ConnectionResponse.ConnectionState.MAYBE_UNKNOWN_ERROR,
        ConnectionResponse.ConnectionState.MAYBE_CONNECTION_ERROR
    ).contains(connectionResponse.current.state)

    private fun isPrinterConnecting(connectionResponse: ConnectionResponse) = listOf(
        ConnectionResponse.ConnectionState.MAYBE_CONNECTING,
        ConnectionResponse.ConnectionState.MAYBE_DETECTING_SERIAL_PORT,
        ConnectionResponse.ConnectionState.MAYBE_DETECTING_SERIAL_CONNECTION,
        ConnectionResponse.ConnectionState.MAYBE_DETECTING_BAUDRATE
    ).contains(connectionResponse.current.state)

    private fun isPrinterConnected(connectionResponse: ConnectionResponse) = connectionResponse.current.port != null ||
            connectionResponse.current.baudrate != null

    private fun autoConnect(connectionResponse: ConnectionResponse) = viewModelScope.launch(coroutineExceptionHandler) {
        if (connectionResponse.options.ports.isNotEmpty() && !didJustAttemptToConnect() && !isPrinterConnecting(connectionResponse)) {
            recordConnectionAttempt()
            Timber.i("Attempting auto connect")
            autoConnectPrinterUseCase.execute(
                if (connectionResponse.current.state == MAYBE_ERROR_FAILED_TO_AUTODETECT_SERIAL_PORT) {
                    // TODO Ask user which port to select
                    val app = Injector.get().app()
                    Toast.makeText(app, app.getString(R.string.connect_printer___auto_selection_failed), Toast.LENGTH_SHORT).show()
                    OctoAnalytics.logEvent(OctoAnalytics.Event.PrinterAutoConnectFailed)
                    Params(connectionResponse.options.ports.first())
                } else {
                    Params()
                }
            )
            psuCyclingState.postValue(PsuCycledState.NotCycled)
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

    fun setDeviceOn(device: PowerDevice, on: Boolean) = viewModelScope.launch(coroutineExceptionHandler) {
        val wasPsuTurnedOn = manualPsuState.value
        try {
            manualPsuState.postValue(on)
        } catch (e: Exception) {
            manualPsuState.postValue(wasPsuTurnedOn)
            throw e
        }
    }

    fun cyclePsu(device: PowerDevice) = viewModelScope.launch(coroutineExceptionHandler) {
        psuCyclingState.postValue(PsuCycledState.Cycled)
        manualPsuState.postValue(true)
        resetConnectionAttempt()
    }

    fun retryConnectionFromOfflineState() {
        lastConnectionAttempt = 0L
        psuCyclingState.postValue(PsuCycledState.Cycled)
    }

    fun beginConnect() {
        Timber.i("Connection initiated")
        userAllowedConnectAt = System.currentTimeMillis()
        manualTrigger.postValue(Unit)
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
        object WaitingForUser : UiState()
        object PrinterConnecting : UiState()
        data class PrinterOffline(val psuSupported: Boolean) : UiState()
        object PrinterPsuCycling : UiState()
        object PrinterConnected : UiState()

        object Unknown : UiState()

    }
}