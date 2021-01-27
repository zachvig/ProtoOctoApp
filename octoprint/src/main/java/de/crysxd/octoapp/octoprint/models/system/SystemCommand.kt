package de.crysxd.octoapp.octoprint.models.system

data class SystemCommand(
    val action: String,
    val name: String,
    val confirm: String?,
    val source: String,
    val resource: String
)