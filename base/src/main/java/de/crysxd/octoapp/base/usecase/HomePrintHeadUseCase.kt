package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.octoprint.OctoPrint
import de.crysxd.octoapp.octoprint.models.printer.PrintHeadCommand

class HomePrintHeadUseCase : UseCase<Pair<OctoPrint, HomePrintHeadUseCase.Axis>, Unit> {

    sealed class Axis(val printHeadCommand: PrintHeadCommand) {
        object All : Axis(PrintHeadCommand.HomeAllAxisPrintHeadCommand)
        object XY : Axis(PrintHeadCommand.HomeXYAxisPrintHeadCommand)
        object Z : Axis(PrintHeadCommand.HomeZAxisPrintHeadCommand)
    }

    override suspend fun execute(param: Pair<OctoPrint, Axis>) {
        param.first.createPrinterApi().executePrintHeadCommand(param.second.printHeadCommand)
    }
}