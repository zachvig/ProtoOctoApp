package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.octoprint.models.printer.GcodeCommand
import timber.log.Timber
import javax.inject.Inject

class ChangeFilamentUseCase @Inject constructor(
    private val octoPrintProvider: OctoPrintProvider
) : UseCase<Unit, Unit>() {

    override suspend fun doExecute(param: Unit, timber: Timber.Tree) {
        octoPrintProvider.octoPrint().createPrinterApi().executeGcodeCommand(GcodeCommand.Single("M600"))
    }
}