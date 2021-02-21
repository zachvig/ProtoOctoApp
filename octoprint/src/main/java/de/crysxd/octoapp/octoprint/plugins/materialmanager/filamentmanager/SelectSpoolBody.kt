package de.crysxd.octoapp.octoprint.plugins.materialmanager.filamentmanager

data class SelectSpoolBody(
    val selection: Selection
) {
    data class Selection(
        val tool: Int = 0,
        val spool: Spool
    )

    data class Spool(
        val id: String
    )
}