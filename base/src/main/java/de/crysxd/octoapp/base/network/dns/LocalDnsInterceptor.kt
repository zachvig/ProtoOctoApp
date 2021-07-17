package de.crysxd.octoapp.base.network.dns

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
            retryWithLocalDns(chain, request)
        }
    }

    private fun retryWithLocalDns(chain: Interceptor.Chain, request: Request) = try {
        val host = request.url.host
        val ip = localDnsResolver.resolve(host)
        val url = request.url.newBuilder().host(ip).build()
        val upgradedRequest = request.newBuilder().url(url).build()
        Timber.w("Resolved ${request.url.host} locally")
        chain.proceed(upgradedRequest)
    } catch (e: UnknownHostException) {
        Timber.v("System failed to resolve ${request.url.host} and also local resolution failed")
        throw e
    }
}
