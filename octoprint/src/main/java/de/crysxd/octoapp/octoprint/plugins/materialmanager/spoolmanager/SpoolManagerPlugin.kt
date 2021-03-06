package de.crysxd.octoapp.octoprint.plugins.materialmanager.spoolmanager

import de.crysxd.octoapp.octoprint.plugins.materialmanager.Material
import de.crysxd.octoapp.octoprint.plugins.materialmanager.MaterialManagerPlugin

class SpoolManagerPlugin(private val spoolManagerApi: SpoolManagerApi) : MaterialManagerPlugin {
    override val pluginId = "SpoolManager"

    override suspend fun getMaterials(): List<Material> {
        val response = spoolManagerApi.listSpools()
        return response.allSpools.map {
            Material(
                id = it.databaseId,
                displayName = it.displayName,
                color = it.color,
                vendor = it.vendor ?: "Unknown",
                material = it.material ?: "Unknown",
                pluginDisplayName = "SpoolManager",
                pluginId = pluginId,
                isActivated = response.selectedSpools?.filterNotNull()?.any { s ->
                    s.databaseId == it.databaseId
                } == true
            )
        }
    }

    override suspend fun activateSpool(id: String) = spoolManagerApi.selectSpool(SelectSpoolBody(id.toInt()))
}
