package de.crysxd.octoapp.base.network.dns

import android.content.Context
import android.net.wifi.WifiManager
import com.qiniu.android.dns.DnsManager
import com.qiniu.android.dns.NetworkInfo
import com.qiniu.android.dns.local.AndroidDnsServer
import com.qiniu.android.dns.local.Resolver
import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.base.utils.measureTime
import kotlinx.coroutines.*
import timber.log.Timber
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class LocalDnsResolver(private val context: Context) {

    companion object {
        private val cache = mutableListOf<DnsEntry>()
        private const val CACHE_ENTRY_TTL = 600_000L
        private const val RESOLVE_TIMEOUT = 3
        private val resolveLock = ReentrantLock()
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

        // Resolve. We do this in a coroutine scope and execute the resolve async so we can limit the time effectively
        measureTime("local_dns_resolve") {
            doResolve(hostName)
        }
    }

    private fun doResolve(hostName: String): String {
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
            dnsIps.map { Resolver(InetAddress.getByName(it), RESOLVE_TIMEOUT) }
        ).flatten().toTypedArray()

        // Resolve!
        val res = try {
            val dns = DnsManager(NetworkInfo.normal, resolvers)
            dns.queryErrorHandler = DnsManager.QueryErrorHandler { e, host -> Timber.w("Unable to resolve host $host (${e::class.java.simpleName}: ${e.message})") }
            dns.query(hostName).firstOrNull() ?: throw UnknownHostException("Resolving $hostName resulted in null")
        } catch (e: Exception) {
            Timber.e(e, "Unable to resolve host $hostName")
            throw UnknownHostException(hostName)
        }

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