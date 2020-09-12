package de.crysxd.octoapp.signin.models

import java.io.Serializable
import java.security.cert.Certificate

data class SignInInformation(
    val webUrl: String,
    val apiKey: String,
    val trustedCerts: List<Certificate>? = null
) : Serializable