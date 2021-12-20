package de.crysxd.octoapp.octoprint

import de.crysxd.octoapp.octoprint.exceptions.IllegalBasicAuthConfigurationException
import de.crysxd.octoapp.octoprint.exceptions.InvalidPathException
import de.crysxd.octoapp.octoprint.models.ConnectionType
import okhttp3.Credentials
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.util.regex.Pattern

const val UPNP_ADDRESS_PREFIX = "octoprint-via-upnp---"

fun HttpUrl.withoutBasicAuth() = newBuilder()
    .username("")
    .password("")
    .build()

fun HttpUrl.resolvePath(path: String?) = path?.let {
    // This is similar to the old behaviour when we did not use HttpUrl
    val sanitized = (newBuilder().query(null).toString().removeSuffix("/") + "/").toHttpUrl()
    sanitized.newBuilder(it)?.build() ?: throw InvalidPathException()
} ?: this

fun HttpUrl.forLogging() = newBuilder()
    .host(redactedHost)
    .password(if (password.isNotEmpty()) "\$basicAuthPassword" else "")
    .username(if (username.isNotEmpty()) "\$basicAuthUser" else "")
    .build()

fun HttpUrl.redactLoggingString(log: String) = log.replaceIfNotEmpty(host, redactedHost)
    .replaceIfNotEmpty(password, "\$basicAuthsPassword")
    .replaceIfNotEmpty(username, "\$basicAuthUser")
    .replaceIfNotEmpty(encodedPassword, "\$basicAuthsPassword")
    .replaceIfNotEmpty(encodedUsername, "\$basicAuthUser")

private val HttpUrl.redactedHost
    get() = when {
        // Host is local IP address (class A, B and C)
        Pattern.compile("^192\\.168\\.\\d+\\.\\d+$").matcher(host).matches() -> host
        Pattern.compile("((^172\\.1[6-9]\\.)|(^172\\.2[0-9]\\.)|(^172\\.3[0-1]\\.))\\d+\\.\\d+$").matcher(host).matches() -> host
        Pattern.compile("^10\\.\\d+\\.\\d+\\.\\d+$").matcher(host).matches() -> host

        // Host is UPnP, mDNS or or home domain
        host.endsWith(".local") -> host
        host.endsWith(".home") -> host
        host.startsWith(UPNP_ADDRESS_PREFIX) -> host

        // OctoEverywhere and ngrok
        host.endsWith(".octoeverywhere.com") -> String.format("redacted-%x.octoeverywhere.com", host.hashCode())
        host.endsWith(".ngrok.com") -> String.format("redacted-%x.ngrok.com", host.hashCode())
        host.endsWith(".tunnels.app.thespaghettidetective.com") -> String.format("redacted-%x.tunnels.app.thespaghettidetective.com", host.hashCode())

        // All other cases. Redact.
        else -> String.format("redacted-host-%x", host.hashCode())
    }

fun HttpUrl.extractAndRemoveBasicAuth(): Pair<HttpUrl, String?> {
    val header = if (username.isNotBlank()) {
        try {
            Credentials.basic(username, password)
        } catch (e: Exception) {
            throw IllegalBasicAuthConfigurationException(this)
        }
    } else {
        null
    }

    return withoutBasicAuth() to header
}

fun HttpUrl.isBasedOn(baseUrl: HttpUrl?) = baseUrl != null &&
        withoutBasicAuth().toString().addTrailingSlash().startsWith(baseUrl.withoutBasicAuth().toString().addTrailingSlash())

private fun String.addTrailingSlash() = removeSuffix("/") + "/"

private fun String.replaceIfNotEmpty(needle: String, replacement: String) = if (needle.isEmpty()) {
    this
} else {
    replace(needle, replacement)
}

fun HttpUrl.isOctoEverywhereUrl() = host.endsWith(".octoeverywhere.com")

fun HttpUrl.isSpaghettiDetectiveUrl() = host.endsWith("thespaghettidetective.com")

fun HttpUrl.getConnectionType(default: ConnectionType) = when {
    isOctoEverywhereUrl() -> ConnectionType.OctoEverywhere
    isSpaghettiDetectiveUrl() -> ConnectionType.SpaghettiDetective
    isNgrokUrl() -> ConnectionType.Ngrok
    isTailscale() -> ConnectionType.Tailscale
    else -> default
}

fun HttpUrl.isSharedOctoEverywhereUrl() = host.startsWith("shared-") && host.endsWith(".octoeverywhere.com")

fun HttpUrl.isNgrokUrl() = host.endsWith(".ngrok.io")

fun HttpUrl.isTailscale() = Pattern.compile("^100.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$").matcher(host).matches()

fun HttpUrl.isHlsStreamUrl() = pathSegments.lastOrNull()?.let { it.endsWith(".m3u") || it.endsWith(".m3u8") } ?: false
