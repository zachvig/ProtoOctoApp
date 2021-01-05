package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.octoprint.plugins.power.PowerDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import javax.inject.Inject

class GetPowerDevicesUseCase @Inject constructor(
    private val octoPrintRepository: OctoPrintRepository,
    private val octoPrintProvider: OctoPrintProvider
) : UseCase<GetPowerDevicesUseCase.Params, Flow<List<Pair<PowerDevice, Boolean?>>>>() {

    override suspend fun doExecute(param: Params, timber: Timber.Tree) = flow<List<Pair<PowerDevice, Boolean?>>> {
        val settings = octoPrintRepository.getRawOctoPrintInstanceInformation()?.settings
            ?: octoPrintProvider.octoPrint().createSettingsApi().getSettings()
        val devices = octoPrintProvider.octoPrint().createPowerPluginsCollection().getDevices(settings)

        // Emit without power state
        emit(devices.map { Pair(it, null) })

        // If we should query power state do so and emit a second value
        if (param.queryState) {
            // Use withContext to split the stream in parallel
            val withPowerState = devices.map {
                try {
                    Pair(it, it.isOn())
                } catch (e: Exception) {
                    Timber.e(e)
                    Pair(it, null)
                }
            }

            // Emit unified list after all have been collected
            emit(withPowerState)
        }
    }

    data class Params(
        val queryState: Boolean
    )
}