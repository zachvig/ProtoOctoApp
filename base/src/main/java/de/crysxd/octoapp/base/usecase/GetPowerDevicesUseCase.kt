package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.octoprint.plugins.power.PowerDevice
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject

@Suppress("EXPERIMENTAL_API_USAGE")
class GetPowerDevicesUseCase @Inject constructor(
    private val octoPrintRepository: OctoPrintRepository,
    private val octoPrintProvider: OctoPrintProvider
) : UseCase<GetPowerDevicesUseCase.Params, Flow<List<Pair<PowerDevice, GetPowerDevicesUseCase.PowerState>>>>() {

    override suspend fun doExecute(param: Params, timber: Timber.Tree) = octoPrintRepository.instanceInformationFlow().map {
        it?.settings
    }.distinctUntilChanged().map {
        flow {
            val devices = it?.let {
                octoPrintProvider.octoPrint().createPowerPluginsCollection().getDevices(it)
            } ?: emptyList()

            // Emit without power state
            val result = devices.map { Pair(it, if (param.queryState) PowerState.Loading else PowerState.Unknown) }
                .toMap()
                .toMutableMap()
            emit(result.toList())

            // If we should query power state do so and emit a second value
            if (param.queryState) {
                // Use withContext to split the stream in parallel
                devices.forEach {
                    try {
                        result[it] = if (it.isOn()) PowerState.On else PowerState.Off
                    } catch (e: Exception) {
                        Timber.e(e)
                        result[it] = PowerState.Unknown
                    }
                    emit(result.toList())
                }
            }
        }
    }.flatMapLatest { it }

    data class Params(
        val queryState: Boolean
    )

    sealed class PowerState {
        object On : PowerState()
        object Off : PowerState()
        object Unknown : PowerState()
        object Loading : PowerState()
    }
}