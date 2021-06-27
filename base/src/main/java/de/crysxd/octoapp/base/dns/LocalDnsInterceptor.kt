package de.crysxd.octoapp.base.dns

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import timber.log.Timber
import java.net.UnknownHostException

/**
 * Android is a little stupid when it comes to resolving *.local and *.home domains, sometimes it bypasses the local router as DNS server.
 *
 * This interceptor will catch a UnknownHostException and will attempt to resolve the hostname with a custom DNS resolver which will contact the router directly but
 * also uses the standard Android resolver as a fallback. After the host was resolved, the request is continued with the resolved IP. This class also contains a simple
 * TTL based cache to speed up consecutive requests.
 */
class LocalDnsInterceptor(
    private val localDnsResolver: LocalDnsResolver
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        return try {
            chain.proceed(request)
        } catch (e: UnknownHostException) {
            Timber.w("Unable to resolve ${request.url.host}, attempting local DNS resolution")
            retryWithLocalDns(chain, request)
        }
    }

    private fun retryWithLocalDns(chain: Interceptor.Chain, request: Request, attempt: Int = 0): Response {
        val host = request.url.host
        val ip = localDnsResolver.resolve(host)
        val url = request.url.newBuilder().host(ip).build()
        val upgradedRequest = request.newBuilder().url(url).build()
        return chain.proceed(upgradedRequest)
    }
}
