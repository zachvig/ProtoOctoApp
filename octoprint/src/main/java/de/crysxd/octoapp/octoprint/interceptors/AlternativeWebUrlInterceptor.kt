package de.crysxd.octoapp.octoprint.interceptors

import de.crysxd.octoapp.octoprint.isOctoEverywhereUrl
import de.crysxd.octoapp.octoprint.models.ConnectionType
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.util.logging.Level
import java.util.logging.Logger


class AlternativeWebUrlInterceptor(
    private val logger: Logger,
    private val webUrl: String,
    private val alternativeWebUrl: String?
) : Interceptor {

    var isPrimaryUsed = true

    override fun intercept(chain: Interceptor.Chain): Response {
        return doIntercept(chain, 0)
    }

    private fun doIntercept(chain: Interceptor.Chain, attempt: Int): Response {
        var usingPrimary = isPrimaryUsed
        try {
            val request = chain.request()
            val upgradedRequest = if (alternativeWebUrl != null && !isPrimaryUsed) {
                val url = request.url.toString()
                usingPrimary = false
                request.newBuilder().url(url.replace(webUrl, alternativeWebUrl)).build()
            } else {
                usingPrimary = true
                request
            }

            return chain.proceed(upgradedRequest)
        } catch (e: IOException) {
            if (attempt < 1 && canSolveExceptionBySwitchingUrl(e)) {
                isPrimaryUsed = !usingPrimary
                logger.log(Level.WARNING, "Caught exception, switching web url to ${if (isPrimaryUsed) "primary" else "alternative"}")
                return doIntercept(chain, 1)
            } else {
                throw e
            }
        }
    }

    fun getActiveConnectionType() = when {
        isPrimaryUsed && webUrl.isOctoEverywhereUrl() -> ConnectionType.OctoEverywhere
        isPrimaryUsed || alternativeWebUrl == null -> ConnectionType.Primary
        alternativeWebUrl.isOctoEverywhereUrl() -> ConnectionType.OctoEverywhere
        else -> ConnectionType.Alternative
    }

    private fun canSolveExceptionBySwitchingUrl(e: Exception) = e is IOException
}