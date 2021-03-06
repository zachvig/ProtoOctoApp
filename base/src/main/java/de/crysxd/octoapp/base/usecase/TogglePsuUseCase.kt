package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.octoprint.plugins.power.PowerDevice
import de.crysxd.octoapp.octoprint.plugins.power.runWithPowerPluginFixes
import timber.log.Timber
import javax.inject.Inject

class TogglePsuUseCase @Inject constructor() : UseCase<PowerDevice, Unit>() {

    override suspend fun doExecute(param: PowerDevice, timber: Timber.Tree) = runWithPowerPluginFixes(param) {
        OctoAnalytics.logEvent(OctoAnalytics.Event.PsuToggled)
        param.toggle()
    }
}