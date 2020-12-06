package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.base.OctoPrintProvider
import kotlinx.coroutines.delay
import timber.log.Timber
import javax.inject.Inject

class CyclePsuUseCase @Inject constructor(
    private val octoPrintProvider: OctoPrintProvider
) : UseCase<Unit, Unit>() {

    override suspend fun doExecute(param: Unit, timber: Timber.Tree) {
        OctoAnalytics.logEvent(OctoAnalytics.Event.PsuCycled)
        octoPrintProvider.octoPrint().createPsuApi().apply {
            turnPsuOff()
            delay(1000)
            turnPsuOn()
            delay(1000)
        }
    }
}