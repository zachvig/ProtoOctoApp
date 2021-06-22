package de.crysxd.octoapp.base

import android.content.Context
import android.net.wifi.WifiManager
import com.qiniu.android.dns.DnsManager
import com.qiniu.android.dns.NetworkInfo
import com.qiniu.android.dns.local.AndroidDnsServer
import com.qiniu.android.dns.local.Resolver
import de.crysxd.octoapp.base.utils.measureTime
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import timber.log.Timber
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Android is a little stupid when it comes to resolving *.local and *.home domains. This is due to a "Private DNS" settings that
 * is depending on the manufacturer set. If enabled, this will bypass the local router as a DNS and directly head to 8.8.8.8 via DNS over HTTPS. As the
 * public DNS has no clue about our *.local and *.home domain, it can't be resolved.
 *
 * This interceptor will catch a UnknownHostException and will attempt to resolve the hostname with a custom DNS resolver which will contact the router directly but
 * also uses the standard Android resolver as a fallback. After the host was resolved, the request is continued with the resolved IP. This class also contains a simple
 * TTL based cache to speed up consecutive requests.
 */
class LocalDnsInterceptor(
    private val context: Context
) : Interceptor {

    companion object {
        private val cache = mutableListOf<DnsEntry>()
        private const val CACHE_ENTRY_TTL = 600_000L
        private val resolveLock = ReentrantLock()
    }

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
        val ip = resolve(host)
        val url = request.url.newBuilder().host(ip).build()
        val upgradedRequest = request.newBuilder().url(url).build()
        return chain.proceed(upgradedRequest)
    }

    fun resolve(hostName: String): String = resolveLock.withLock {
        // Clean up cache
        val now = Date()
        cache.removeAll { it.validUntil < now }

        // Check cache
        cache.firstOrNull { it.hostName == hostName }?.let {
            Timber.v("Cache hit for $hostName=${it.resolvedIp}")
            return@withLock it.resolvedIp
        }

        measureTime("local_dns_resolve") {
            // From the WiFi manager, get the gateway and DHCP server address, also replace last octet of own ip address with 1
            // Both usually is the router
            // For good measure, also add common router IPs in case the user has a double DHCP issue
            val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val dnsIps = listOf(
                wifi.dhcpInfo.gateway.asIpString(),
                wifi.dhcpInfo.serverAddress.asIpString(),
                wifi.dhcpInfo.ipAddress.asIpString().split(".").toMutableList().also { it[3] = "1" }.joinToString("."),
                "192.168.0.1",
                "192.168.1.1",
                "192.168.2.1",
            ).distinct()

            Timber.i("Using as DNS server: $dnsIps")

            // Use the standard Android resolver as fallback
            // Add all DNS server from above
            val resolvers = listOf(
                listOf(AndroidDnsServer.defaultResolver(context)),
                dnsIps.map { Resolver(InetAddress.getByName(it)) }
            ).flatten().toTypedArray()

            // Resolve!
            val dns = DnsManager(NetworkInfo.normal, resolvers)
            val res = dns.query(hostName).firstOrNull() ?: throw UnknownHostException("Resolving $hostName resulted in null")
            Timber.i("Resolved ${hostName}=$res")
            OctoAnalytics.logEvent(OctoAnalytics.Event.InbuiltDnsResolveSuccess)
            cache.add(
                DnsEntry(
                    hostName = hostName,
                    resolvedIp = res,
                    validUntil = Date(System.currentTimeMillis() + CACHE_ENTRY_TTL)
                )
            )
            return res
        }
    }

    private fun Int.asIpString(): String {
        var ip = this
        return (0..3).joinToString(".") {
            val octet = (ip and 0xff).toString()
            ip = ip shr 8
            octet
        }
    }

    private data class DnsEntry(
        val hostName: String,
        val resolvedIp: String,
        val validUntil: Date,
    )
}
