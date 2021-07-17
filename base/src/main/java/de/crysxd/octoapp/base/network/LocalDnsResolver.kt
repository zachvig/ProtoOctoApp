package de.crysxd.octoapp.base.network

import android.content.Context
import android.net.wifi.WifiManager
import com.qiniu.android.dns.DnsManager
import com.qiniu.android.dns.NetworkInfo
import com.qiniu.android.dns.local.Resolver
import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.base.network.OctoPrintUpnpDiscovery.Companion.UPNP_ADDRESS_PREFIX
import de.crysxd.octoapp.base.utils.measureTime
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import okhttp3.Dns
import timber.log.Timber
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class LocalDnsResolver(private val context: Context) : Dns {

    companion object {
        private val cache = mutableListOf<DnsEntry>()
        private const val CACHE_ENTRY_TTL = 600_000L
        private const val RESOLVE_TIMEOUT = 3L
        private val resolveLock = ReentrantLock()
    }

    override fun lookup(hostname: String): List<InetAddress> {
        if (hostname.startsWith(UPNP_ADDRESS_PREFIX)) {
            return localLookup(hostname)
        }

        return try {
            InetAddress.getAllByName(hostname).toList()
        } catch (e: UnknownHostException) {
            Timber.v("Android failed to resolve address, falling back")
            localLookup(hostname)
        }
    }

    private fun localLookup(hostname: String) = resolveLock.withLock {
        // Clean up cache
        val now = Date()
        cache.removeAll { it.validUntil < now }

        // Check cache
        cache.firstOrNull { it.hostname == hostname }?.let {
            Timber.v("Cache hit for $hostname=${it.resolvedIp}")
            return@withLock it.resolvedIp
        }
        Timber.v("Cache miss for $hostname")

        // Resolve. We do this in a coroutine scope and execute the resolve async so we can limit the time effectively
        val result = when {
            hostname.startsWith(UPNP_ADDRESS_PREFIX) -> measureTime("local_upnp_dns_lookup") {
                doUpnpLookup(hostname)
            }

            hostname.endsWith(".local") || hostname.endsWith(".home") -> measureTime("local_mdns_lookup") {
                doMDnsLookup(hostname)
            }
            else -> measureTime("local_dns_lookup") {
                doDnsLookup(hostname)
            }
        }

        // Add to cache
        addCacheEntry(
            DnsEntry(
                hostname = hostname,
                resolvedIp = result,
                validUntil = Date(System.currentTimeMillis() + CACHE_ENTRY_TTL)
            )
        )

        result
    }

    private fun doUpnpLookup(upnpHostname: String): List<InetAddress> = runBlocking {
        Timber.i("Resolving via UPnP: $upnpHostname")
        withTimeoutOrNull(TimeUnit.SECONDS.toMillis(RESOLVE_TIMEOUT)) {
            val channel = Channel<InetAddress>()
            val job = GlobalScope.launch {
                OctoPrintUpnpDiscovery(context).discover {
                    if (it.upnpHostname == upnpHostname) {
                        channel.offer(it.address)
                    }
                }
            }
            val address = channel.receive()
            job.cancel()
            listOf(address)
        } ?: throw UnknownHostException(upnpHostname)
    }

    private fun doMDnsLookup(upnpHostname: String): List<InetAddress> = runBlocking {
        Timber.i("Resolving via mDns: $upnpHostname")
        withTimeoutOrNull(TimeUnit.SECONDS.toMillis(RESOLVE_TIMEOUT)) {
            val channel = Channel<InetAddress>()
            val job = GlobalScope.launch {
                OctoPrintDnsSdDiscovery(context).discover(currentCoroutineContext()) {
                    val names = listOf(
                        upnpHostname,
                        "${upnpHostname.split(".")[0]}.home",
                        "${upnpHostname.split(".")[0]}.lan",
                    )
                    if (names.contains(it.host.hostName)) {
                        channel.offer(it.host)
                    }
                }
            }
            val address = channel.receive()
            job.cancel()
            listOf(address)
        } ?: throw UnknownHostException(upnpHostname)
    }

    private fun doDnsLookup(hostname: String): List<InetAddress> {
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
        val resolvers = dnsIps.map { Resolver(InetAddress.getByName(it), RESOLVE_TIMEOUT.toInt()) }.toTypedArray()

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

    fun addUpnpDeviceToCache(upnpDevice: OctoPrintUpnpDiscovery.Device) {
        // Directly add all devices we found to cache
        addCacheEntry(
            DnsEntry(
                hostname = upnpDevice.upnpHostname,
                resolvedIp = listOf(upnpDevice.address),
                validUntil = Date(System.currentTimeMillis() + CACHE_ENTRY_TTL)
            )
        )
    }

    fun addMdnsDeviceToCache(mdnsDevice: OctoPrintDnsSdDiscovery.Device) {
        // Directly add all devices we found to cache
        addCacheEntry(
            DnsEntry(
                hostname = mdnsDevice.host.hostName,
                resolvedIp = listOf(mdnsDevice.host),
                validUntil = Date(System.currentTimeMillis() + CACHE_ENTRY_TTL)
            )
        )
    }

    private fun addCacheEntry(entry: DnsEntry) {
        cache.removeAll { it.hostname == entry.hostname }
        cache.add(entry)
    }

    private data class DnsEntry(
        val hostname: String,
        val resolvedIp: List<InetAddress>,
        val validUntil: Date,
    )
}