package de.crysxd.octoapp.octoprint.exceptions

import java.io.IOException
import java.net.URL
import java.util.logging.Level
import java.util.logging.Logger

open class OctoPrintException(
    cause: Throwable? = null,
    open val userFacingMessage: String? = null,
    val technicalMessage: String? = userFacingMessage,
    val webUrl: String?,
    val apiKey: String? = null
) : IOException(mask(technicalMessage, webUrl, apiKey), cause?.let { ProxyException.create(it, webUrl, apiKey) }) {

    companion object {
        private fun mask(input: String?, webUrl: String?, apiKey: String? = null): String {
            var output = input ?: ""

            createSensitiveData(webUrl, apiKey).forEach {
                output = output.replace(it.key, "\${${it.value}}")
            }
            return output
        }

        private fun createSensitiveData(webUrl: String?, apiKey: String?) = try {
            val url = webUrl?.let { URL(it) }
            mapOf(
                url?.host to "octoprint_host",
                apiKey to "api_key",
                url?.userInfo to "octoprint_user_info"
            )
        } catch (e: Exception) {
            Logger.getGlobal().log(Level.SEVERE, "Unable to extract sensitive data", e)
            mapOf(
                webUrl to "octoprint_host",
                apiKey to "api_key",
            )
        }.mapNotNull {
            if (it.key != null) it.key!! to it.value else null
        }.toMap()
    }
}