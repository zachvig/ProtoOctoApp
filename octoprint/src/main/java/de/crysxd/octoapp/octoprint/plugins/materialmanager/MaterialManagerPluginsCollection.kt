package de.crysxd.octoapp.octoprint.plugins.materialmanager

import de.crysxd.octoapp.octoprint.models.settings.Settings
import de.crysxd.octoapp.octoprint.plugins.materialmanager.filamentmanager.FilamentManagerPlugin
import de.crysxd.octoapp.octoprint.plugins.materialmanager.spoolmanager.SpoolManagerApi
import de.crysxd.octoapp.octoprint.plugins.materialmanager.spoolmanager.SpoolManagerPlugin
import retrofit2.Retrofit

class MaterialManagerPluginsCollection(retrofit: Retrofit) {

    val plugins = listOf(
        SpoolManagerPlugin(retrofit.create(SpoolManagerApi::class.java)),
        FilamentManagerPlugin(),
    )

    suspend fun getMaterials(settings: Settings) = plugins.filter {
        settings.plugins.containsKey(it.pluginId)
    }.map {
        it.getMaterials()
    }.flatten()

    suspend fun activateMaterial(uniqueId: String) {
        val (pluginId, materialId) = uniqueId.split(":")
        plugins.first { it.pluginId == pluginId }.activateSpool(materialId)
    }
}
