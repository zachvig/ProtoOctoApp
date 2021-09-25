package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.base.network.OctoPrintProvider
import timber.log.Timber
import javax.inject.Inject

class ActivateMaterialUseCase @Inject constructor(private val octoPrintProvider: OctoPrintProvider) : UseCase<ActivateMaterialUseCase.Params, Unit>() {

    override suspend fun doExecute(param: Params, timber: Timber.Tree) {
        octoPrintProvider.octoPrint().createMaterialManagerPluginsCollection().activateMaterial(param.uniqueMaterialId)
    }

    data class Params(
        val uniqueMaterialId: String
    )
}