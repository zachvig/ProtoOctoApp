package de.crysxd.octoapp.octoprint.plugins.materialmanager.filamentmanager

import de.crysxd.octoapp.octoprint.plugins.materialmanager.Material
import de.crysxd.octoapp.octoprint.plugins.materialmanager.MaterialManagerPlugin

class FilamentManagerPlugin : MaterialManagerPlugin {
    override val pluginId = "filamentmanager"

    override suspend fun getMaterials(): List<Material> {
        TODO("Not yet implemented")
    }

    override suspend fun activateSpool(id: String) {
        TODO("Not yet implemented")
    }

}