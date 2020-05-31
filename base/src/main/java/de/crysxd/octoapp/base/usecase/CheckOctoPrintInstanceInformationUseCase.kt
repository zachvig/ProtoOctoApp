package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.base.models.OctoPrintInstanceInformation

class CheckOctoPrintInstanceInformationUseCase : UseCase<OctoPrintInstanceInformation, Boolean> {

    override suspend fun execute(param: OctoPrintInstanceInformation) =
        param.port > 0 && param.hostName.isNotBlank() && param.apiKey.isNotBlank()

}