package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.octoprint.models.connection.ConnectionResponse
import timber.log.Timber
import javax.inject.Inject

class GetPrinterConnectionUseCase @Inject constructor(
    private val octoPrintProvider: OctoPrintProvider
) : UseCase<Unit, ConnectionResponse>() {

    override suspend fun doExecute(param: Unit, timber: Timber.Tree) =
        octoPrintProvider.octoPrint().createConnectionApi().getConnection()
}