package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.octoprint.models.printer.PrintHeadCommand
import timber.log.Timber
import javax.inject.Inject

class HomePrintHeadUseCase @Inject constructor(
    private val octoPrintProvider: OctoPrintProvider
) : UseCase<HomePrintHeadUseCase.Axis, Unit>() {

    sealed class Axis(val printHeadCommand: PrintHeadCommand) {
        object All : Axis(PrintHeadCommand.HomeAllAxisPrintHeadCommand)
        object XY : Axis(PrintHeadCommand.HomeXYAxisPrintHeadCommand)
        object Z : Axis(PrintHeadCommand.HomeZAxisPrintHeadCommand)
    }

    override suspend fun doExecute(param: Axis, timber: Timber.Tree) {
        octoPrintProvider.octoPrint().createPrinterApi().executePrintHeadCommand(param.printHeadCommand)
    }
}