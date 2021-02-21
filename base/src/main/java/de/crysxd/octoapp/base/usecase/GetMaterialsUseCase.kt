package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.octoprint.plugins.materialmanager.Material
import timber.log.Timber
import javax.inject.Inject

class GetMaterialsUseCase @Inject constructor(
    private val octoPrintProvider: OctoPrintProvider,
    private val octoPrintRepository: OctoPrintRepository
) : UseCase<Unit, List<Material>>() {

    override suspend fun doExecute(param: Unit, timber: Timber.Tree): List<Material> {
        val octoPrint = octoPrintProvider.octoPrint()
        val settings = octoPrintRepository.getActiveInstanceSnapshot()?.settings
            ?: octoPrint.createSettingsApi().getSettings()
        return octoPrint.createMaterialManagerPluginsCollection().getMaterials(settings)
    }
}