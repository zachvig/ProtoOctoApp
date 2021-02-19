package de.crysxd.octoapp.octoprint.exceptions

import java.io.IOException
import java.net.URL
import java.util.logging.Level
import java.util.logging.Logger

class ProxyException(original: Throwable, webUrl: String, apiKey: String? = null) : IOException(
    mask(original.message, webUrl, apiKey),
    proxyCause(original.cause, webUrl, apiKey)
) {
    val originalMessage = original.localizedMessage

    init {
        stackTrace = original.stackTrace
    }

    companion object {
        private fun proxyCause(exception: Throwable?, url: String, apiKey: String?) = exception?.let { ProxyException(it, url, apiKey) }

        fun mask(input: String?, webUrl: String, apiKey: String? = null): String {
            var output = input ?: ""

            createSensitiveData(webUrl, apiKey).forEach {
                output = output.replace(it.key, "\${${it.value}}")
            }
            return output
        }

        private fun createSensitiveData(webUrl: String, apiKey: String?) = try {
            val url = URL(webUrl)
            listOf(
                url.host to "octoprint_host",
                apiKey to "api_key",
                url.userInfo to "octoprint_user_info"
            ).mapNotNull {
                it.takeIf { it.first != null }
            }.toMap()
        } catch (e: Exception) {
            Logger.getGlobal().log(Level.SEVERE, "Unable to extract sensitive data", e)
            mapOf(
                webUrl to "octoprint_host",
                apiKey to "api_key",
            )
        }
    }
}