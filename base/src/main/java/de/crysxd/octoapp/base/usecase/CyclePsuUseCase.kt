package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.octoprint.plugins.power.PowerDevice
import kotlinx.coroutines.delay
import timber.log.Timber
import javax.inject.Inject

class CyclePsuUseCase @Inject constructor() : UseCase<PowerDevice, Unit>() {

    override suspend fun doExecute(param: PowerDevice, timber: Timber.Tree) {
        OctoAnalytics.logEvent(OctoAnalytics.Event.PsuCycled)
        val e = try {
            param.turnOff()
            null
        } catch (e: Exception) {
            timber.e(e, "Error while turning off, continuing anyways")
            e
        }
        delay(2000)
        param.turnOn()
        delay(1000)
        e?.let { throw it }
    }
}