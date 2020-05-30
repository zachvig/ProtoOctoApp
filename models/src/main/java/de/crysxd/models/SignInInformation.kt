package de.crysxd.models

import java.io.Serializable

data class SignInInformation(
    val ipAddress: CharSequence,
    val port: CharSequence,
    val apiKey: CharSequence
) : Serializable