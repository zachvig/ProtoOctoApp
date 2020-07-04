package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.octoprint.OctoPrint
import de.crysxd.octoapp.octoprint.models.connection.ConnectionResponse
import javax.inject.Inject

class GetPrinterConnectionUseCase @Inject constructor() : UseCase<OctoPrint, ConnectionResponse> {

    override suspend fun execute(param: OctoPrint) = param.createConnectionApi().getConnection()

}