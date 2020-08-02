package de.crysxd.octoapp.signin.models

import java.io.Serializable

data class SignInInformation(
    val webUrl: String,
    val apiKey: String
) : Serializable