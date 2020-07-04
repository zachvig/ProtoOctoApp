package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.octoprint.OctoPrint
import de.crysxd.octoapp.octoprint.models.connection.ConnectionCommand
import javax.inject.Inject

class AutoConnectPrinterUseCase @Inject constructor() : UseCase<OctoPrint, Unit> {

    override suspend fun execute(param: OctoPrint) {
        param.createConnectionApi().executeConnectionCommand(ConnectionCommand.Connect())
    }
}