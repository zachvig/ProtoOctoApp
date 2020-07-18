package de.crysxd.octoapp.base.models

data class OctoPrintInstanceInformation(
    val hostName: String,
    val port: Int,
    val apiKey: String,
    val supportsPsuPlugin: Boolean = false
)