package de.crysxd.octoapp.octoprint.models.login

data class LoginResponse(
    val session: String,
    val name: String,
    val groups: List<String>?,
    val isAdmin: Boolean
) {
    companion object {
        const val GROUP_USERS = "users"
        const val GROUP_ADMINS = "admins"
    }
}