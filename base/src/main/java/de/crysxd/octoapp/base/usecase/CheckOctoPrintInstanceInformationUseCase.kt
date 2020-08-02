package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.base.models.OctoPrintInstanceInformationV2
import javax.inject.Inject

class CheckOctoPrintInstanceInformationUseCase @Inject constructor() : UseCase<OctoPrintInstanceInformationV2, Boolean> {

    override suspend fun execute(param: OctoPrintInstanceInformationV2) =
        param.webUrl.isNotBlank() && param.apiKey.isNotBlank()

}