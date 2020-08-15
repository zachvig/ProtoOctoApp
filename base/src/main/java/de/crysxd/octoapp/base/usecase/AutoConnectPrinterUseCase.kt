package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.octoprint.OctoPrint
import de.crysxd.octoapp.octoprint.models.connection.ConnectionCommand
import timber.log.Timber
import javax.inject.Inject

const val AUTO_PORT = "AUTO"

class AutoConnectPrinterUseCase @Inject constructor() : UseCase<AutoConnectPrinterUseCase.Params, Unit>() {

    override suspend fun doExecute(param: Params, timber: Timber.Tree) {
        param.octoPrint.createConnectionApi().executeConnectionCommand(
            ConnectionCommand.Connect(
                port = param.port
            )
        )
    }

    data class Params(
        val octoPrint: OctoPrint,
        val port: String = AUTO_PORT
    )
}