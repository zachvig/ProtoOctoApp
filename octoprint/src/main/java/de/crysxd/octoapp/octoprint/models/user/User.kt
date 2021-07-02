package de.crysxd.octoapp.octoprint.models.user

data class User(
    val permissions: List<String>,
    val groups: List<String>,
    val name: String
) {
    val isGuest get() = groups.contains("guests")
    val canAccessSystemCommands get() = permissions.contains("SYSTEM")
}