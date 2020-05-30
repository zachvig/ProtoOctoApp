package de.crysxd.octoapp.signin.models

import java.io.Serializable

data class SignInInformation(
    val ipAddress: String,
    val port: String,
    val apiKey: String
) : Serializable