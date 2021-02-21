package de.crysxd.octoapp.octoprint.plugins.materialmanager.filamentmanager

import de.crysxd.octoapp.octoprint.plugins.materialmanager.Material
import de.crysxd.octoapp.octoprint.plugins.materialmanager.MaterialManagerPlugin

class FilamentManagerPlugin(private val filamentManagerApi: FilamentManagerApi) : MaterialManagerPlugin {
    override val pluginId = "filamentmanager"

    override suspend fun getMaterials() = filamentManagerApi.listSpools().spools.map {
        Material(
            id = it.id,
            displayName = it.name,
            color = null,
            pluginDisplayName = "FilamentManager",
            pluginId = pluginId
        )
    }

    override suspend fun activateSpool(id: String) = filamentManagerApi.selectSpool(
        SelectSpoolBody(
            SelectSpoolBody.Selection(
                tool = 0,
                spool = SelectSpoolBody.Spool(id)
            )
        )
    )
}