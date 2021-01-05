package de.crysxd.octoapp.base.ui.common.power

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import de.crysxd.octoapp.base.usecase.GetPowerDevicesUseCase
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow

@Suppress("EXPERIMENTAL_API_USAGE")
class SelectPowerDeviceViewModel(
    getPowerDevicesUseCase: GetPowerDevicesUseCase
) : ViewModel() {

    val powerDevices = flow {
        emit(getPowerDevicesUseCase.execute(GetPowerDevicesUseCase.Params(true)))
    }.flatMapLatest { it }.distinctUntilChanged().asLiveData()

}