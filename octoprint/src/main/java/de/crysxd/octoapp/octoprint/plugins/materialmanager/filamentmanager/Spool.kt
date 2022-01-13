package de.crysxd.octoapp.octoprint.plugins.materialmanager.filamentmanager

data class Spool(
    val id: String,
    val name: String,
    val profile: Profile,
    val weight: Float? = null,
) {
    data class Profile(
        val vendor: String?,
        val material: String?,
    )
}