package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.octoprint.OctoPrint
import de.crysxd.octoapp.octoprint.models.connection.ConnectionResponse
import timber.log.Timber
import javax.inject.Inject

class GetPrinterConnectionUseCase @Inject constructor() : UseCase<OctoPrint, ConnectionResponse?>() {

    override suspend fun doExecute(param: OctoPrint, timber: Timber.Tree) = param.createConnectionApi().getConnection()
}