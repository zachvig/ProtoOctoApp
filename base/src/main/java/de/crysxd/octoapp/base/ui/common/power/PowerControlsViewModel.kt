package de.crysxd.octoapp.base.ui.common.power

import androidx.lifecycle.*
import de.crysxd.octoapp.base.ui.BaseViewModel
import de.crysxd.octoapp.base.usecase.*
import de.crysxd.octoapp.octoprint.plugins.power.PowerDevice
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@Suppress("EXPERIMENTAL_API_USAGE")
class PowerControlsViewModel(
    getPowerDevicesUseCase: GetPowerDevicesUseCase,
    private val turnOnPsuUseCase: TurnOnPsuUseCase,
    private val turnOffPsuUseCase: TurnOffPsuUseCase,
    private val cyclePsuUseCase: CyclePsuUseCase
) : BaseViewModel() {

    private val mutableViewState = MediatorLiveData<ViewState>()
    val viewState = mutableViewState.map { it }

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
                mutableViewState.postValue(ViewState.PowerDevicesLoaded(it))
            }
        }
    }

    fun executeAction(device: PowerDevice, action: PowerControlsBottomSheet.Action) = viewModelScope.launch(coroutineExceptionHandler) {
        // Only accept commands if we are not yet busy or done
        if (mutableViewState.value !is ViewState.PowerDevicesLoaded) {
            return@launch
        }

        mutableViewState.postValue(ViewState.Loading)
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