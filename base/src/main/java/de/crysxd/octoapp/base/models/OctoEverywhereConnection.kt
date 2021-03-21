package de.crysxd.octoapp.base.models

import javassist.bytecode.annotation.StringMemberValue

data class OctoEverywhereConnection(
    val connectionId: String,
    val apiToken: String,
    val bearerToken: String,
    val basicAuthUser: String,
    val basicAuthPassword: String,
    val url: String,
    val fullUrl: String,
)