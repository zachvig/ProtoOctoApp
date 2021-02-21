package de.crysxd.octoapp.octoprint.plugins.materialmanager

data class Material(
    val id: String,
    val pluginId: String,
    val displayName: String,
    val color: String?,
    val pluginDisplayName: String,
    val isActivated: Boolean
) {
    val uniqueId
        get() = "$pluginId:$id"
}