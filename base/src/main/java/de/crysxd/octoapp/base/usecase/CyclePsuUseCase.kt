package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.octoprint.plugins.power.PowerDevice
import de.crysxd.octoapp.octoprint.plugins.power.runWithPowerPluginFixes
import kotlinx.coroutines.delay
import timber.log.Timber
import javax.inject.Inject

class CyclePsuUseCase @Inject constructor() : UseCase<PowerDevice, Unit>() {

    override suspend fun doExecute(param: PowerDevice, timber: Timber.Tree) {
        OctoAnalytics.logEvent(OctoAnalytics.Event.PsuCycled)
        runWithPowerPluginFixes(param) {
            param.turnOff()
        }
        delay(2000)
        runWithPowerPluginFixes(param) {
            param.turnOn()
        }
    }
}