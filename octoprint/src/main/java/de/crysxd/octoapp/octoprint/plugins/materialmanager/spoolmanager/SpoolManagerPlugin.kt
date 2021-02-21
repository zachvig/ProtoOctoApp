package de.crysxd.octoapp.octoprint.plugins.materialmanager.spoolmanager

import de.crysxd.octoapp.octoprint.plugins.materialmanager.Material
import de.crysxd.octoapp.octoprint.plugins.materialmanager.MaterialManagerPlugin

class SpoolManagerPlugin(private val spoolManagerApi: SpoolManagerApi) : MaterialManagerPlugin {
    override val pluginId = "SpoolManager"

    override suspend fun getMaterials() = spoolManagerApi.listSpools().allSpools.map {
        Material(
            id = it.databaseId,
            displayName = it.displayName,
            color = it.color,
            pluginDisplayName = "SpoolManager",
            pluginId = pluginId
        )
    }

    override suspend fun activateSpool(id: String) = spoolManagerApi.selectSpool(SelectSpoolBody(id))
}
