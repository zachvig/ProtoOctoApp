package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.base.OctoPrintProvider
import timber.log.Timber
import javax.inject.Inject

class TurnOnPsuUseCase @Inject constructor(
    private val octoPrintProvider: OctoPrintProvider
) : UseCase<Unit, Unit>() {

    override suspend fun doExecute(param: Unit, timber: Timber.Tree) {
        OctoAnalytics.logEvent(OctoAnalytics.Event.PsuTurnedOn)
        octoPrintProvider.octoPrint().createPsuApi().turnPsuOn()
    }
}