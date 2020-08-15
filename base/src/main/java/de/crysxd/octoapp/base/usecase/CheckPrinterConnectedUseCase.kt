package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.octoprint.OctoPrint
import de.crysxd.octoapp.octoprint.exceptions.PrinterNotOperationalException
import timber.log.Timber
import javax.inject.Inject

class CheckPrinterConnectedUseCase @Inject constructor() : UseCase<OctoPrint, Boolean>() {

    override suspend fun doExecute(param: OctoPrint, timber: Timber.Tree) = try {
        param.createPrinterApi().getPrinterState()
        true
    } catch (e: PrinterNotOperationalException) {
        false
    }
}