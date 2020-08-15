package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.octoprint.models.connection.ConnectionCommand
import timber.log.Timber
import javax.inject.Inject

const val AUTO_PORT = "AUTO"

class AutoConnectPrinterUseCase @Inject constructor(
    private val octoPrintProvider: OctoPrintProvider
) : UseCase<AutoConnectPrinterUseCase.Params, Unit>() {

    override suspend fun doExecute(param: Params, timber: Timber.Tree) {
        octoPrintProvider.octoPrint().createConnectionApi().executeConnectionCommand(
            ConnectionCommand.Connect(
                port = param.port
            )
        )
    }

    data class Params(
        val port: String = AUTO_PORT
    )
}