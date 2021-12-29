package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.base.data.repository.OctoPrintRepository
import de.crysxd.octoapp.base.network.OctoPrintProvider
import de.crysxd.octoapp.octoprint.models.printer.PrintHeadCommand
import de.crysxd.octoapp.octoprint.models.profiles.PrinterProfiles
import timber.log.Timber
import javax.inject.Inject

class JogPrintHeadUseCase @Inject constructor(
    private val octoPrintProvider: OctoPrintProvider,
    private val octoPrintRepository: OctoPrintRepository,
) : UseCase<JogPrintHeadUseCase.Param, Unit>() {

    data class Param(
        val xDistance: Float = 0f,
        val yDistance: Float = 0f,
        val zDistance: Float = 0f,
        val speedMmMin: Int = 4000
    )

    private val PrinterProfiles.Axis?.multiplier get() = if (this?.inverted == true) -1f else 1f

    override suspend fun doExecute(param: Param, timber: Timber.Tree) {
        val axes = octoPrintRepository.getActiveInstanceSnapshot()?.activeProfile?.axes

        octoPrintProvider.octoPrint().createPrinterApi().executePrintHeadCommand(
            PrintHeadCommand.JogPrintHeadCommand(
                x = param.xDistance * axes?.x.multiplier,
                y = param.yDistance * axes?.y.multiplier,
                z = param.zDistance * axes?.z.multiplier,
                speed = param.speedMmMin
            )
        )
    }
}