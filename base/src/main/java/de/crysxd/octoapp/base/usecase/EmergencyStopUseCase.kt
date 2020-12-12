package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.octoprint.models.printer.GcodeCommand
import timber.log.Timber
import javax.inject.Inject

class EmergencyStopUseCase @Inject constructor(
    private val octoPrintProvider: OctoPrintProvider
) : UseCase<Unit, Unit>() {

    override suspend fun doExecute(param: Unit, timber: Timber.Tree) {
        OctoAnalytics.logEvent(OctoAnalytics.Event.EmergencyStopTriggeredByApp)
        octoPrintProvider.octoPrint().createPrinterApi().executeGcodeCommand(GcodeCommand.Single("M112"))
    }
}