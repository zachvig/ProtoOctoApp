package de.crysxd.octoapp.octoprint.plugins.materialmanager

data class Material(
    val id: String,
    val pluginId: String,
    val displayName: String,
    val vendor: String,
    val material: String,
    val color: String?,
    val colorName: String?,
    val pluginDisplayName: String,
    val isActivated: Boolean,
    val weightGrams: Float?,
) {
    val uniqueId
        get() = "$pluginId:$id"
}