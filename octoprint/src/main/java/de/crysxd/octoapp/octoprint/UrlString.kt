package de.crysxd.octoapp.octoprint

import de.crysxd.octoapp.octoprint.exceptions.IllegalBasicAuthConfigurationException
import okhttp3.Credentials
import java.net.URI
import java.net.URL
import java.net.URLDecoder

typealias UrlString = String

fun UrlString.sanitizeUrl() = "${this.trim().removeSuffix("/")}/"

fun UrlString?.isFullUrl() = this?.trim()?.startsWith("http") == true


internal fun UrlString.removeUserInfo(): UrlString {
    val start = indexOf("://")
    val end = indexOf("@")
    return if (start > 0 && end > 0) {
        replaceRange(start + 3, end + 1, "")
    } else {
        this
    }
}

fun UrlString.extractAndRemoveUserInfo(): Pair<String, String?> {
    val header = URI(this.trim()).rawUserInfo?.let {
        try {
            val components = it.split(":")
            Credentials.basic(components[0].urlDecode(), components.getOrNull(1)?.urlDecode() ?: "")
        } catch (e: Exception) {
            throw IllegalBasicAuthConfigurationException(this)
        }
    }
    val url = removeUserInfo()

    return url to header
}

fun UrlString.isOctoEverywhereUrl() = URL(this.trim()).host.contains("octoeverywhere.com", ignoreCase = true)

fun String.urlDecode() = URLDecoder.decode(this, "UTF-8")