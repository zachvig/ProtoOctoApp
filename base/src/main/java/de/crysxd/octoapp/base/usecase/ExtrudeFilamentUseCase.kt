package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.octoprint.models.printer.ToolCommand
import timber.log.Timber
import javax.inject.Inject

class ExtrudeFilamentUseCase @Inject constructor(
    private val octoPrintProvider: OctoPrintProvider
) : UseCase<ExtrudeFilamentUseCase.Param, Unit>() {

    override suspend fun doExecute(param: Param, timber: Timber.Tree) {
        octoPrintProvider.octoPrint().createPrinterApi().executeToolCommand(
            ToolCommand.ExtrudeFilamentToolCommand(param.extrudeLengthMm)
        )
    }

    data class Param(
        val extrudeLengthMm: Int
    )
}