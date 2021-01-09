package de.crysxd.octoapp.base.ui.common.power

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.*
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
    getPowerDevicesUseCase: GetPowerDevicesUseCase,
    private val turnOnPsuUseCase: TurnOnPsuUseCase,
    private val turnOffPsuUseCase: TurnOffPsuUseCase,
    private val cyclePsuUseCase: CyclePsuUseCase,
    private val sharedPreferences: SharedPreferences,
) : BaseViewModel() {

    private val mutableViewState = MediatorLiveData<ViewState>()
    val viewState = mutableViewState.map { it }

    private var autoAction: PowerControlsBottomSheet.Action? = null
    private var autoDeviceType: PowerControlsBottomSheet.DeviceType? = null

    init {
        var isFirst = true
        val powerDevices = flow {
            emit(getPowerDevicesUseCase.execute(GetPowerDevicesUseCase.Params(true)))
        }.flatMapLatest { it }
            .onEach {
                if (isFirst) {
                    // Load at least 300 ms to make the initial load animation nice
                    isFirst = false
                    delay(300)
                }
            }
            .distinctUntilChanged()
            .asLiveData()

        // Update thr power devices once while loading (initial load) and while we still
        // show them and not moved to either loading state again or completed state
        var isInitialLoad = true
        mutableViewState.postValue(ViewState.Loading)
        mutableViewState.addSource(powerDevices) {
            if (isInitialLoad || mutableViewState.value is ViewState.PowerDevicesLoaded) {
                isInitialLoad = false
                if (!attemptAutoHandle(it)) {
                    mutableViewState.postValue(ViewState.PowerDevicesLoaded(it))
                }
            }
        }
    }

    fun setAction(action: PowerControlsBottomSheet.Action, deviceType: PowerControlsBottomSheet.DeviceType) {
        autoAction = action
        autoDeviceType = deviceType
        attemptAutoHandle((viewState.value as? ViewState.PowerDevicesLoaded)?.powerDevices)
    }

    private fun attemptAutoHandle(devices: PowerDeviceList?): Boolean {
        val action = autoAction
        val deviceType = autoDeviceType ?: PowerControlsBottomSheet.DeviceType.Unspecified
        val defaultDeviceIdForAction = sharedPreferences.getString(createPreferencesKeyForActionId(deviceType), null)

        return if (action != null && devices != null && defaultDeviceIdForAction != null) {
            devices.firstOrNull { it.first.uniqueId == defaultDeviceIdForAction }?.first?.let {
                executeAction(it, action, deviceType, useAsDefault = true, forceExecute = true)
                true
            } ?: false
        } else {
            false
        }
    }

    private fun createPreferencesKeyForActionId(deviceType: PowerControlsBottomSheet.DeviceType) =
        "power_controls_default_${deviceType::class.java.simpleName.toLowerCase(Locale.ENGLISH)}"

    fun executeAction(
        device: PowerDevice,
        action: PowerControlsBottomSheet.Action,
        deviceType: PowerControlsBottomSheet.DeviceType,
        useAsDefault: Boolean,
        forceExecute: Boolean = false
    ) = viewModelScope.launch(coroutineExceptionHandler) {
        // Only accept commands if we are not yet busy or done
        if (!forceExecute && (mutableViewState.value !is ViewState.PowerDevicesLoaded)) {
            Timber.w("Dropped execution, already busy")
            return@launch
        }

        // Store default :)
        if (deviceType != PowerControlsBottomSheet.DeviceType.Unspecified && useAsDefault) {
            sharedPreferences.edit { putString(createPreferencesKeyForActionId(deviceType), device.uniqueId) }
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
            }
            mutableViewState.postValue(ViewState.Completed(action, device))
        } catch (e: Exception) {
            mutableViewState.postValue(ViewState.Completed(null, device))
            throw e
        }
    }

    sealed class ViewState {
        object Loading : ViewState()
        data class PowerDevicesLoaded(val powerDevices: PowerDeviceList) : ViewState()
        data class Completed(val action: PowerControlsBottomSheet.Action?, val powerDevice: PowerDevice) : ViewState()
    }
}