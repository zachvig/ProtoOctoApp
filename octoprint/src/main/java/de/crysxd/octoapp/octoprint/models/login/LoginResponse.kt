package de.crysxd.octoapp.octoprint.models.login

data class LoginResponse(
    val session: String,
    val name: String,
    val isAdmin: Boolean
)