package de.crysxd.octoapp.octoprint

import de.crysxd.octoapp.octoprint.exceptions.IllegalBasicAuthConfigurationException
import okhttp3.Credentials
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.net.URI
import java.net.URL
import java.net.URLDecoder

typealias UrlString = String

fun UrlString.sanitizeUrl() = toHttpUrl().toString()

fun UrlString?.isFullUrl() = try {
    this?.toHttpUrl() != null
} catch (e: Exception) {
    false
}

fun UrlString.removeUserInfo(): UrlString = toHttpUrl().newBuilder().username("").password("").build().toString()

fun UrlString.extractAndRemoveUserInfo(): Pair<String, String?> {
    val url = toHttpUrl()
    val username = url.username
    val password = url.password

    val header = if (username.isNotBlank()) {
        try {
            Credentials.basic(username, password)
        } catch (e: Exception) {
            throw IllegalBasicAuthConfigurationException(this)
        }
    } else {
        null
    }

    return removeUserInfo() to header
}

fun UrlString.isOctoEverywhereUrl() = try {
    URL(this.trim()).host.contains("octoeverywhere.com", ignoreCase = true)
} catch (e: Exception) {
    false
}