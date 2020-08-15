package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.base.models.OctoPrintInstanceInformationV2
import timber.log.Timber
import javax.inject.Inject

class CheckOctoPrintInstanceInformationUseCase @Inject constructor() : UseCase<OctoPrintInstanceInformationV2, Boolean>() {

    override suspend fun doExecute(param: OctoPrintInstanceInformationV2, timber: Timber.Tree) =
        param.webUrl.isNotBlank() && param.apiKey.isNotBlank()

}