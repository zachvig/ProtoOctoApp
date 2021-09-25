package de.crysxd.octoapp.base.data.models

import okhttp3.HttpUrl

data class OctoEverywhereConnection(
    val connectionId: String,
    val apiToken: String,
    val bearerToken: String,
    val basicAuthUser: String,
    val basicAuthPassword: String,
    val url: HttpUrl,
    val fullUrl: HttpUrl,
)