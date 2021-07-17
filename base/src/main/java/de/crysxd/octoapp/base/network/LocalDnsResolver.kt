package de.crysxd.octoapp.base.network

import android.content.Context
import android.net.wifi.WifiManager
import com.qiniu.android.dns.DnsManager
import com.qiniu.android.dns.NetworkInfo
import com.qiniu.android.dns.local.Resolver
import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.base.utils.measureTime
import kotlinx.coroutines.*
import okhttp3.Dns
import timber.log.Timber
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class LocalDnsResolver(private val context: Context) : Dns {

    companion object {
        private val cache = mutableListOf<DnsEntry>()
        private const val CACHE_ENTRY_TTL = 600_000L
        private const val RESOLVE_TIMEOUT = 3
        private val resolveLock = ReentrantLock()
    }

    override fun lookup(hostname: String): List<InetAddress> = resolveLock.withLock {
        // Clean up cache
        val now = Date()
        cache.removeAll { it.validUntil < now }

        // Check cache
        cache.firstOrNull { it.hostname == hostname }?.let {
            Timber.v("Cache hit for $hostname=${it.resolvedIp}")
            return@withLock it.resolvedIp
        }

        // Resolve. We do this in a coroutine scope and execute the resolve async so we can limit the time effectively
        measureTime("local_dns_lookup") {
            doLookup(hostname)
        }
    }

    private fun doLookup(hostname: String): List<InetAddress> {
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

        // Add all DNS server from above
        val resolvers = dnsIps.map { Resolver(InetAddress.getByName(it), RESOLVE_TIMEOUT) }.toTypedArray()

        // Resolve!
        val res = try {
            val dns = DnsManager(NetworkInfo.normal, resolvers)
            dns.queryErrorHandler = DnsManager.QueryErrorHandler { e, host -> Timber.w("Unable to resolve host $host (${e::class.java.simpleName}: ${e.message})") }
            dns.query(hostname).map { InetAddress.getByName(it) }
        } catch (e: Exception) {
            Timber.e(e, "Unable to resolve host $hostname")
            throw UnknownHostException(hostname)
        }

        if (res.isEmpty()) {
            Timber.w("No results for $hostname")
            throw UnknownHostException(hostname)
        }

        Timber.i("Resolved ${hostname}=$res")
        OctoAnalytics.logEvent(OctoAnalytics.Event.InbuiltDnsResolveSuccess)
        cache.add(
            DnsEntry(
                hostname = hostname,
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
        val hostname: String,
        val resolvedIp: List<InetAddress>,
        val validUntil: Date,
    )
}