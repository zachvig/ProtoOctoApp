package de.crysxd.octoapp.octoprint.plugins.materialmanager

interface MaterialManagerPlugin {

    val pluginId: String

    suspend fun getMaterials(): List<Material>
    suspend fun activateSpool(id: String)

}