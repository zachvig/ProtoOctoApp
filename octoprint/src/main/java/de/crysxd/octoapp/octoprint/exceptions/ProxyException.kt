package de.crysxd.octoapp.octoprint.exceptions

import kotlinx.coroutines.CancellationException
import java.io.IOException
import java.net.URL
import java.util.logging.Level
import java.util.logging.Logger

/*
 * ProxyException is a construct to ensure data privacy in logs and crash reports. ProxyExceptions is a replacement for the
 * original exception and can be used for low-level network errors. ProxyException will srub the error message and the error message
 * of any cause while keeping the original stack trace and exception structure.
 *
 * Certain exceptions like CancelledException or InterruptedException are deemed save and are not proxied as they are essential for
 * the program flow.
 */
class ProxyException private constructor(val original: Throwable, webUrl: String, apiKey: String? = null) : IOException(
    "${mask(original.message, webUrl, apiKey)} [Proxy for ${original::class.java.name}, URL: $webUrl]",
    original.cause?.let { create(it, webUrl, apiKey) }
) {
    init {
        stackTrace = original.stackTrace
    }

    companion object {
        private fun isAcceptedException(original: Throwable?) =
            original is CancellationException || original is InterruptedException

        fun create(original: Throwable, webUrl: String, apiKey: String? = null) = if (isAcceptedException(original)) {
            original
        } else {
            ProxyException(original, webUrl, apiKey)
        }

        private fun proxyCause(exception: Throwable?, url: String, apiKey: String?) =
            exception?.let { create(it, url, apiKey) }

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