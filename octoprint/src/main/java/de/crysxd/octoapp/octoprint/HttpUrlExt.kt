package de.crysxd.octoapp.octoprint

import de.crysxd.octoapp.octoprint.exceptions.IllegalBasicAuthConfigurationException
import okhttp3.Credentials
import okhttp3.HttpUrl
import java.lang.IllegalStateException

fun HttpUrl.withoutBasicAuth() = newBuilder()
    .username("")
    .password("")
    .build()

fun HttpUrl.resolvePath(path: String?) = path?.let {  newBuilder(path)?.build() } ?: this
    ?: throw IllegalStateException("Builder was null")

fun HttpUrl.extractAndRemoveBasicAuth(): Pair<HttpUrl, String?> {
    val header = if (username.isNotBlank()) {
        try {
            Credentials.basic(username, password)
        } catch (e: Exception) {
            throw IllegalBasicAuthConfigurationException(this.toString())
        }
    } else {
        null
    }

    return withoutBasicAuth() to header
}

fun HttpUrl.isBasedOn(baseUrl: HttpUrl) = toString().startsWith(baseUrl.toString())

fun HttpUrl.isOctoEverywhereUrl() = host.endsWith("octoeverywhere.com")