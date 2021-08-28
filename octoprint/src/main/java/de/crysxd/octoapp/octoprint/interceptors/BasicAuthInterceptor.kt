package de.crysxd.octoapp.octoprint.interceptors

import de.crysxd.octoapp.octoprint.extractAndRemoveBasicAuth
import de.crysxd.octoapp.octoprint.isBasedOn
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import java.util.logging.Level
import java.util.logging.Logger

class BasicAuthInterceptor(private val logger: Logger, vararg baseUrls: HttpUrl?) : Interceptor {
    private val credentials = baseUrls
        .filterNotNull()
        .map { it.extractAndRemoveBasicAuth() }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url

        // Do we have credentials for this URL? If so, add them
        val upgradedRequest = credentials.firstOrNull {
            url.isBasedOn(it.first)
        }?.second?.let {
            logger.log(Level.FINEST, "Adding authorization for $url")
            request.newBuilder()
                .header("Authorization", it)
                .build()
        } ?: request

        return chain.proceed(upgradedRequest)
    }
}