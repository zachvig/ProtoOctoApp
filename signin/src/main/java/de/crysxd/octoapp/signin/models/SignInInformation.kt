package de.crysxd.octoapp.signin.models

import java.io.Serializable

data class SignInInformation(
    val ipAddress: CharSequence,
    val port: CharSequence,
    val apiKey: CharSequence
) : Serializable