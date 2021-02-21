package de.crysxd.octoapp.octoprint.plugins.materialmanager.filamentmanager

data class SelectionsResponse(
    val selections: List<Selection>
) {
    data class Selection(
        val tool: Int,
        val spool: Spool
    )
}