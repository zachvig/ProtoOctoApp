package de.crysxd.octoapp.octoprint.models.system

data class SystemCommandList(
    val core: List<SystemCommand>?,
    val custom: List<SystemCommand>?
) {
    val all get() = listOfNotNull(core, custom).flatten()
}
