package de.crysxd.octoapp.base.ui.common.power

import androidx.lifecycle.*
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.base.ui.BaseViewModel
import de.crysxd.octoapp.base.usecase.*
import de.crysxd.octoapp.octoprint.plugins.power.PowerDevice
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*

@Suppress("EXPERIMENTAL_API_USAGE")
class PowerControlsViewModel(
    private val getPowerDevicesUseCase: GetPowerDevicesUseCase,
    private val turnOnPsuUseCase: TurnOnPsuUseCase,
    private val turnOffPsuUseCase: TurnOffPsuUseCase,
    private val cyclePsuUseCase: CyclePsuUseCase,
    private val octoPrintRepository: OctoPrintRepository,
) : BaseViewModel() {

    private val mutableViewState = MediatorLiveData<ViewState>()
    val viewState = mutableViewState.map { it }

    private var autoAction: PowerControlsBottomSheet.Action? = null
    private var autoDeviceType: PowerControlsBottomSheet.DeviceType? = null
    private var isInitialised = false

    fun setAction(action: PowerControlsBottomSheet.Action, deviceType: PowerControlsBottomSheet.DeviceType) = viewModelScope.launch(coroutineExceptionHandler) {
        autoAction = action
        autoDeviceType = deviceType

        // Get devices without state, this is a instant action from cache
        // Try to perform the action with the cache value
        try {
            val simpleDevices = getPowerDevicesUseCase.execute(GetPowerDevicesUseCase.Params(false))
            if (attemptAutoHandle(simpleDevices.first())) {
                Timber.i("Used default device for $action")
                return@launch
            }
        } catch (e: Exception) {
            coroutineExceptionHandler.handleException(coroutineContext, e)
            Timber.e(e)
            Timber.i("Default device action failed. Clearing default")
            octoPrintRepository.updateAppSettingsForActive {
                it.copy(defaultPowerDevices = it.defaultPowerDevices?.toMutableMap()?.apply { remove(deviceType.prefKey) })
            }
        }

        // We couldn't use the default, load power devices and query on/off state
        // This function might get called more than once. Do not query state again
        if (!isInitialised) {
            isInitialised = true
            var isFirst = true
            val start = System.currentTimeMillis()
            val powerDevices = flow {
                emit(getPowerDevicesUseCase.execute(GetPowerDevicesUseCase.Params(true)))
            }.flatMapLatest { it }
                .onEach {
                    if (isFirst) {
                        // Load at least 500 ms to make the initial load animation nice
                        delay((500 - (System.currentTimeMillis() - start)).coerceAtLeast(0))
                    }

                    // Only update UI if this is the first load or we still show the devices
                    // We must not jump from a second locaing state (when action is executed) or from completed back to devices
                    if (isFirst || mutableViewState.value is ViewState.PowerDevicesLoaded) {
                        mutableViewState.postValue(ViewState.PowerDevicesLoaded(it))
                    }

                    isFirst = false
                }
                .asLiveData()

            // Hook up flow to view state
            mutableViewState.addSource(powerDevices) { }
        }
    }

    private suspend fun attemptAutoHandle(devices: PowerDeviceList?): Boolean {
        val action = autoAction
        val deviceType = autoDeviceType ?: PowerControlsBottomSheet.DeviceType.Unspecified
        val defaultDeviceIdForAction = octoPrintRepository.getActiveInstanceSnapshot()?.appSettings?.defaultPowerDevices?.get(deviceType.prefKey)

        return if ((action != null && action != PowerControlsBottomSheet.Action.Unspecified) && devices != null && (defaultDeviceIdForAction != null || devices.size == 1)) {
            val device = devices.firstOrNull { it.first.uniqueId == defaultDeviceIdForAction }
                ?: devices.firstOrNull()?.takeIf { devices.size == 1 }

            device?.first?.let {
                executeActionInternal(it, action, deviceType, useAsDefault = true, internalExecution = true)
                true
            } ?: false
        } else {
            false
        }
    }

    fun executeAction(
        device: PowerDevice,
        action: PowerControlsBottomSheet.Action,
        deviceType: PowerControlsBottomSheet.DeviceType,
        useAsDefault: Boolean
    ) = viewModelScope.launch(coroutineExceptionHandler) {
        executeActionInternal(device, action, deviceType, useAsDefault)
    }

    private suspend fun executeActionInternal(
        device: PowerDevice,
        action: PowerControlsBottomSheet.Action,
        deviceType: PowerControlsBottomSheet.DeviceType,
        useAsDefault: Boolean,
        internalExecution: Boolean = false
    ) {
        // Only accept commands if we are not yet busy or done
        if (!internalExecution && (mutableViewState.value !is ViewState.PowerDevicesLoaded)) {
            Timber.w("Dropped execution, already busy")
            return
        }

        // Store default :)
        if (deviceType != PowerControlsBottomSheet.DeviceType.Unspecified && useAsDefault) {
            octoPrintRepository.updateAppSettingsForActive {
                it.copy(defaultPowerDevices = (it.defaultPowerDevices ?: emptyMap()).toMutableMap().apply { this[deviceType.prefKey] = device.uniqueId })
            }
        }

        // Switch to loading state
        mutableViewState.postValue(ViewState.Loading)

        // Well...if we request the state and instantly execute a action the tradfri plugin fries OctoPrint
        if (device.pluginId == "tradfri") {
            delay(500)
        }

        // Execute action and post the completed state with the executed action
        try {
            when (action) {
                PowerControlsBottomSheet.Action.TurnOn -> turnOnPsuUseCase.execute(device)
                PowerControlsBottomSheet.Action.TurnOff -> turnOffPsuUseCase.execute(device)
                PowerControlsBottomSheet.Action.Cycle -> cyclePsuUseCase.execute(device)
                else -> throw IllegalArgumentException("Action is not specified, can't execute")
            }
            mutableViewState.postValue(ViewState.Completed(action, device))
        } catch (e: Exception) {
            // Action failed, thus we report a null action as nothing was done
            // If this internally triggered, we do not post the result as there might be further steps
            if (!internalExecution) {
                mutableViewState.postValue(ViewState.Completed(null, device))
            }

            throw e
        }
    }

    private val PowerControlsBottomSheet.DeviceType.prefKey get() = this::class.java.simpleName.toLowerCase(Locale.ENGLISH)

    sealed class ViewState {
        object Loading : ViewState()
        data class PowerDevicesLoaded(val powerDevices: PowerDeviceList) : ViewState()
        data class Completed(val action: PowerControlsBottomSheet.Action?, val powerDevice: PowerDevice) : ViewState()
    }
}