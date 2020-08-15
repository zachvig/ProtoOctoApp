package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.octoprint.OctoPrint
import de.crysxd.octoapp.octoprint.models.printer.GcodeCommand
import timber.log.Timber
import javax.inject.Inject

class ChangeFilamentUseCase @Inject constructor() : UseCase<OctoPrint, Unit>() {

    override suspend fun doExecute(param: OctoPrint, timber: Timber.Tree) {
        param.createPrinterApi().executeGcodeCommand(GcodeCommand.Single("M600"))
    }
}