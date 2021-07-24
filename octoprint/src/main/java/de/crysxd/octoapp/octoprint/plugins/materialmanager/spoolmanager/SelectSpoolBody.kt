package de.crysxd.octoapp.octoprint.plugins.materialmanager.spoolmanager

data class SelectSpoolBody(
    val databaseId: Int,
    val toolIndex: Int = 0,
)