package de.crysxd.octoapp.octoprint.plugins.materialmanager.filamentmanager

data class ListSpoolResponse(
    val spools: List<Spool>
) {
    data class Spool(
        val id: String,
        val name: String,
    )
}