package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.base.network.OctoPrintProvider
import de.crysxd.octoapp.octoprint.models.printer.PrintHeadCommand
import timber.log.Timber
import javax.inject.Inject

class JogPrintHeadUseCase @Inject constructor(
    private val octoPrintProvider: OctoPrintProvider
) : UseCase<JogPrintHeadUseCase.Param, Unit>() {

    data class Param(
        val xDistance: Float = 0f,
        val yDistance: Float = 0f,
        val zDistance: Float = 0f,
        val speedMmMin: Int = 4000
    )

    override suspend fun doExecute(param: Param, timber: Timber.Tree) {
        octoPrintProvider.octoPrint().createPrinterApi().executePrintHeadCommand(
            PrintHeadCommand.JogPrintHeadCommand(
                param.xDistance,
                param.yDistance,
                param.zDistance,
                param.speedMmMin
            )
        )
    }
}