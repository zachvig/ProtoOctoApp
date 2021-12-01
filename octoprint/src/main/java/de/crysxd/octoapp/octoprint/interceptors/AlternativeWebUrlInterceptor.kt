package de.crysxd.octoapp.octoprint.interceptors

import de.crysxd.octoapp.octoprint.exceptions.AlternativeWebUrlException
import de.crysxd.octoapp.octoprint.exceptions.OctoEverywhereCantReachPrinterException
import de.crysxd.octoapp.octoprint.exceptions.SpaghettiDetectiveCantReachPrinterException
import de.crysxd.octoapp.octoprint.getConnectionType
import de.crysxd.octoapp.octoprint.models.ConnectionType
import de.crysxd.octoapp.octoprint.withoutBasicAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.util.logging.Level
import java.util.logging.Logger


class AlternativeWebUrlInterceptor constructor(
    private val logger: Logger,
    private val fullWebUrl: HttpUrl,
    private val fullAlternativeWebUrl: HttpUrl?
) : Interceptor {

    var isPrimaryUsed = true
        set(value) {
            field = value
            mutableActiveUrl.value = if (value) fullWebUrl else (fullAlternativeWebUrl ?: fullWebUrl)
        }
    private val mutableActiveUrl = MutableStateFlow(fullWebUrl)
    val activeUrl get() = mutableActiveUrl.asStateFlow()

    override fun intercept(chain: Interceptor.Chain): Response {
        return doIntercept(chain, 0)
    }

    private fun doIntercept(chain: Interceptor.Chain, attempt: Int): Response {
        var usingPrimary = isPrimaryUsed
        val request = chain.request()

        try {
            val upgradedRequest = if (fullAlternativeWebUrl != null && !isPrimaryUsed) {
                val url = request.url.toString()
                usingPrimary = false
                val upgradedUrl = url.replace(fullWebUrl.withoutBasicAuth().toString(), fullAlternativeWebUrl.withoutBasicAuth().toString())

                if (upgradedUrl == url && fullWebUrl.withoutBasicAuth() != fullAlternativeWebUrl.withoutBasicAuth()) {
                    throw AlternativeWebUrlException("Alternative URL and primary URL are the same: $url <--> $upgradedUrl", fullWebUrl.withoutBasicAuth())
                }

                request.newBuilder().url(upgradedUrl).build()
            } else {
                usingPrimary = true
                request
            }

            return chain.proceed(upgradedRequest)
        } catch (e: IOException) {
            val isCancelled = chain.call().isCanceled()
            when {
                isCancelled -> {
                    logger.log(Level.INFO, "Caught IOException due to a cancelled, request, ignoring")
                    throw  e
                }

                attempt < 1 && canSolveExceptionBySwitchingUrl(e) -> {
                    isPrimaryUsed = !usingPrimary
                    logger.log(
                        Level.WARNING,
                        "Caught exception in ${request.url}, switching web url to ${if (isPrimaryUsed) "primary" else "alternative"} (${e::class.java.simpleName}: ${e.message})"
                    )
                    logger.log(Level.INFO, "webUrl=$fullWebUrl alternativeWebUrl=$fullAlternativeWebUrl")
                    return doIntercept(chain, 1)
                }

                else -> throw e
            }
        }
    }

    fun getActiveConnectionType() = if (isPrimaryUsed) {
        fullWebUrl.getConnectionType(ConnectionType.Primary)
    } else {
        fullAlternativeWebUrl?.getConnectionType(ConnectionType.Alternative) ?: ConnectionType.Primary
    }

    private fun canSolveExceptionBySwitchingUrl(e: Exception) = e is IOException ||
            e is SpaghettiDetectiveCantReachPrinterException ||
            e is OctoEverywhereCantReachPrinterException
}