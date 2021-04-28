package de.crysxd.octoapp.octoprint

import de.crysxd.octoapp.octoprint.exceptions.IllegalBasicAuthConfigurationException
import okhttp3.Credentials
import java.net.URI
import java.net.URL

typealias UrlString = String

fun UrlString.sanitizeUrl() = "${this.trim().removeSuffix("/")}/"

fun UrlString?.isFullUrl() = this?.trim()?.startsWith("http") == true


internal fun UrlString.removeUserInfo() = URI(this.trim()).let { url ->
    url.userInfo?.let {
        url.toString().replaceFirst("$it@", "")
    } ?: url.toString()
}

fun UrlString.extractAndRemoveUserInfo(): Pair<String, String?> {
    val header = URI(this.trim()).userInfo?.let {
        try {
            val components = it.split(":")
            Credentials.basic(components[0], components.getOrNull(1) ?: "")
        } catch (e: Exception) {
            throw IllegalBasicAuthConfigurationException(this)
        }
    }
    val url = removeUserInfo()

    return url to header
}

fun UrlString.isOctoEverywhereUrl() = URL(this.trim()).host.contains("octoeverywhere.com", ignoreCase = true)