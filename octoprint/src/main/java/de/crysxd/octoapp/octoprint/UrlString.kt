package de.crysxd.octoapp.octoprint

import de.crysxd.octoapp.octoprint.exceptions.IllegalBasicAuthConfigurationException
import okhttp3.Credentials
import java.net.URL

typealias UrlString = String

internal fun UrlString.sanitizeUrl() = "${this.removeSuffix("/")}/"

internal fun UrlString.removeUserInfo() = URL(this).let { url ->
    url.userInfo?.let {
        url.toString().replaceFirst("$it@", "")
    } ?: url.toString()
}

fun UrlString.extractAndRemoveUserInfo(): Pair<String, String?> {
    val header = URL(this).userInfo?.let {
        try {
            val components = it.split(":")
            Credentials.basic(components[0], components[1])
        } catch (e: Exception) {
            throw IllegalBasicAuthConfigurationException(this)
        }
    }
    val url = removeUserInfo()

    return url to header
}

fun UrlString.isOctoEverywhereUrl() = URL(this).host.contains("octoeverywhere.com", ignoreCase = true)