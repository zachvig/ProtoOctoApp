package de.crysxd.octoapp.octoprint.plugins.materialmanager.spoolmanager

data class SelectSpoolBody(
    val databaseId: String,
    val toolIndex: Int = -1,
)