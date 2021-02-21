package de.crysxd.octoapp.octoprint.plugins.materialmanager.spoolmanager

data class ListSpoolResponse(
    val allSpools: List<Spool>,
    val selectedSpool: Spool?
) {
    data class Spool(
        val color: String,
        val colorName: String,
        val databaseId: String,
        val displayName: String,
        val isActive: Boolean,
    )
}