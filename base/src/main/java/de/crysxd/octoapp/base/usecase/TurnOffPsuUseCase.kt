package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.base.usecase.UseCase
import de.crysxd.octoapp.octoprint.OctoPrint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TurnOffPsuUseCase : UseCase<OctoPrint, Unit> {

    override suspend fun execute(param: OctoPrint) = withContext(Dispatchers.IO) {
        param.createPsuApi().turnPsuOff()
    }

}