package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.octoprint.plugins.power.PowerDevice
import timber.log.Timber
import javax.inject.Inject

class TurnOnPsuUseCase @Inject constructor() : UseCase<PowerDevice, Unit>() {

    override suspend fun doExecute(param: PowerDevice, timber: Timber.Tree) {
        OctoAnalytics.logEvent(OctoAnalytics.Event.PsuTurnedOn)
        param.turnOn()
    }
}