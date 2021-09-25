package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.base.network.OctoPrintProvider
import de.crysxd.octoapp.octoprint.exceptions.PrinterNotOperationalException
import timber.log.Timber
import javax.inject.Inject

class CheckPrinterConnectedUseCase @Inject constructor(
    private val octoPrintProvider: OctoPrintProvider
) : UseCase<Unit, Boolean>() {

    override suspend fun doExecute(param: Unit, timber: Timber.Tree) = try {
        octoPrintProvider.octoPrint().createPrinterApi().getPrinterState()
        true
    } catch (e: PrinterNotOperationalException) {
        false
    }
}