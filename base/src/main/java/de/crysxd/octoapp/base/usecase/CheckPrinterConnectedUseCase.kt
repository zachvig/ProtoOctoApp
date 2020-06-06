package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.base.models.exceptions.NoPrinterConnectedException
import de.crysxd.octoapp.octoprint.OctoPrint

class CheckPrinterConnectedUseCase : UseCase<OctoPrint, Boolean> {

    override suspend fun execute(param: OctoPrint) = try {
        param.createPrinterApi().getPrinterState()
        true
    } catch (e: NoPrinterConnectedException) {
        false
    }
}