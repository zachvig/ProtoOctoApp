package de.crysxd.octoapp.octoprint.interceptors

import de.crysxd.octoapp.octoprint.UrlString
import de.crysxd.octoapp.octoprint.extractAndRemoveUserInfo
import okhttp3.Interceptor
import okhttp3.Response
import java.util.logging.Level
import java.util.logging.Logger

class BasicAuthInterceptor(private val logger: Logger, vararg baseUrls: UrlString?) : Interceptor {
    private val credentials = baseUrls
        .filterNotNull()
        .map { it.extractAndRemoveUserInfo() }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url

        // Do we have credentials for this URL? If so, add them
        val upgradedRequest = credentials.firstOrNull {
            url.toString().startsWith(it.first, ignoreCase = true)
        }?.second?.let {
            logger.log(Level.FINEST, "Adding authorization for $url")
            request.newBuilder()
                .header("Authorization", it)
                .build()
        } ?: request

        return chain.proceed(upgradedRequest)
    }
}