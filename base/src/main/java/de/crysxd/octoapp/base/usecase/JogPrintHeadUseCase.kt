package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.octoprint.OctoPrint
import de.crysxd.octoapp.octoprint.models.printer.PrintHeadCommand
import javax.inject.Inject

class JogPrintHeadUseCase @Inject constructor() : UseCase<JogPrintHeadUseCase.Param, Unit> {

    data class Param(
        val octoPrint: OctoPrint,
        val xDistance: Float = 0f,
        val yDistance: Float = 0f,
        val zDistance: Float = 0f,
        val speedMmMin: Int = 4000
    )

    override suspend fun execute(param: Param) {
        param.octoPrint.createPrinterApi().executePrintHeadCommand(
            PrintHeadCommand.JogPrintHeadCommand(
                param.xDistance,
                param.yDistance,
                param.zDistance,
                param.speedMmMin
            )
        )
    }
}