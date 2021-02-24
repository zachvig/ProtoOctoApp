package de.crysxd.octoapp.base.usecase

import android.os.Parcelable
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.octoprint.plugins.power.PowerDevice
import kotlinx.android.parcel.Parcelize
import timber.log.Timber
import javax.inject.Inject

@Suppress("EXPERIMENTAL_API_USAGE")
class GetPowerDevicesUseCase @Inject constructor(
    private val octoPrintRepository: OctoPrintRepository,
    private val octoPrintProvider: OctoPrintProvider
) : UseCase<GetPowerDevicesUseCase.Params, PowerDeviceList>() {

    override suspend fun doExecute(param: Params, timber: Timber.Tree): PowerDeviceList {
        val octoPrint = octoPrintProvider.octoPrint()
        val settings = octoPrintRepository.getActiveInstanceSnapshot()?.settings ?: octoPrint.createSettingsApi().getSettings()
        val devices = octoPrintProvider.octoPrint().createPowerPluginsCollection().getDevices(settings)

        // Emit without power state
        val result = devices.map { Pair<PowerDevice, PowerState>(it, PowerState.Unknown) }
            .filter { param.onlyGetDeviceWithUniqueId == null || param.onlyGetDeviceWithUniqueId == it.first.uniqueId }
            .toMap()
            .toMutableMap()

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
            }
        }

        return result.toList()
    }

    data class Params(
        val queryState: Boolean,
        val onlyGetDeviceWithUniqueId: String? = null
    )

    sealed class PowerState : Parcelable {
        @Parcelize
        object On : PowerState()

        @Parcelize
        object Off : PowerState()

        @Parcelize
        object Unknown : PowerState()
    }
}

typealias PowerDeviceList = List<Pair<PowerDevice, GetPowerDevicesUseCase.PowerState>>
