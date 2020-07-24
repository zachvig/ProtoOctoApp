package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.octoprint.OctoPrint
import de.crysxd.octoapp.octoprint.models.connection.ConnectionResponse
import timber.log.Timber
import javax.inject.Inject

class GetPrinterConnectionUseCase @Inject constructor() : UseCase<OctoPrint, ConnectionResponse?> {

    override suspend fun execute(param: OctoPrint) = try {
        param.createConnectionApi().getConnection()
    } catch (e: Exception) {
        Timber.wtf(e, "[2] Caught exception")
        null
    }
}