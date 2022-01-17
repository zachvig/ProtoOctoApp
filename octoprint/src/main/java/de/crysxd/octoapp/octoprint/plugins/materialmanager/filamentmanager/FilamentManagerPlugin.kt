package de.crysxd.octoapp.octoprint.plugins.materialmanager.filamentmanager

import de.crysxd.octoapp.octoprint.plugins.materialmanager.Material
import de.crysxd.octoapp.octoprint.plugins.materialmanager.MaterialManagerPlugin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

class FilamentManagerPlugin(private val filamentManagerApi: FilamentManagerApi) : MaterialManagerPlugin {
    override val pluginId = "filamentmanager"

    override suspend fun getMaterials() = withContext(Dispatchers.IO) {
        val spools = async { filamentManagerApi.listSpools().spools }
        val selection = filamentManagerApi.getSelections()

        spools.await().filter { (it.weight ?: 0f) > 0 }.map { spool ->
            Material(
                id = spool.id,
                displayName = spool.name,
                color = null,
                colorName = null,
                vendor = spool.profile.vendor ?: "Unknown",
                material = spool.profile.material ?: "Unknown",
                pluginDisplayName = "FilamentManager",
                pluginId = pluginId,
                isActivated = selection.selections.any { it.spool.id == spool.id },
                weightGrams = spool.weight,
            )
        }
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