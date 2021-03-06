package de.crysxd.octoapp.octoprint.exceptions

import kotlinx.coroutines.CancellationException

/*
 * ProxyException is a construct to ensure data privacy in logs and crash reports. ProxyExceptions is a replacement for the
 * original exception and can be used for low-level network errors. ProxyException will srub the error message and the error message
 * of any cause while keeping the original stack trace and exception structure.
 *
 * Certain exceptions like CancelledException or InterruptedException are deemed save and are not proxied as they are essential for
 * the program flow.
 */
class ProxyException private constructor(val original: Throwable, webUrl: String?, apiKey: String? = null) : OctoPrintException(
    originalCause = original.cause,
    userFacingMessage = original.message,
    technicalMessage = "Proxy for ${original::class.java.simpleName}: ${original.message} [url=$webUrl, apiKey=$apiKey]",
    webUrl = webUrl,
    apiKey = apiKey
) {
    init {
        stackTrace = original.stackTrace
    }

    companion object {
        private fun isAcceptedException(original: Throwable?) =
            original is CancellationException || original is InterruptedException

        fun create(original: Throwable, webUrl: String?, apiKey: String? = null) = if (isAcceptedException(original)) {
            original
        } else {
            ProxyException(original, webUrl, apiKey)
        }
    }
}