package de.crysxd.octoapp.base.network

import android.content.Context
import android.net.wifi.WifiManager
import com.github.druk.dnssd.*
import de.crysxd.octoapp.base.di.Injector
import kotlinx.coroutines.job
import timber.log.Timber
import java.net.InetAddress
import kotlin.coroutines.CoroutineContext


class OctoPrintDnsSdDiscovery(
    private val context: Context,
) {
    private val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val dnssd = DNSSDBindable(context)

    fun discover(coroutineContext: CoroutineContext, callback: (Service) -> Unit) {
        val lock = wifi.createMulticastLock("OctoPrintUpnpDiscovery")

        // Sometimes the internal Dnssd service is not running...we can start it with this:
        context.applicationContext.getSystemService(Context.NSD_SERVICE)

        try {
            lock.acquire()
            discoverWithMulticastLock(coroutineContext, callback)
        } catch (e: Exception) {
            Timber.e(e)
        } finally {
            lock.release()
        }
    }

    private fun discoverWithMulticastLock(coroutineContext: CoroutineContext, callback: (Service) -> Unit) {
        val service = dnssd.browse("_octoprint._tcp", object : BrowseListener {
            override fun operationFailed(service: DNSSDService?, errorCode: Int) {
                Timber.e("mDNS browse failed (errorCode=$errorCode)")
            }

            override fun serviceFound(browser: DNSSDService, flags: Int, ifIndex: Int, serviceName: String, regType: String, domain: String) {
                Timber.i("Found $serviceName $regType $domain")
                resolveService(
                    flags = flags,
                    ifIndex = ifIndex,
                    serviceName = serviceName,
                    regType = regType,
                    domain = domain,
                    callback = callback
                )
            }

            override fun serviceLost(browser: DNSSDService?, flags: Int, ifIndex: Int, serviceName: String?, regType: String?, domain: String?) {
                Timber.i("Lost $browser $serviceName $regType $domain")
            }
        })

        coroutineContext.job.invokeOnCompletion {
            service.stop()
        }
    }

    private fun resolveService(flags: Int, ifIndex: Int, serviceName: String, regType: String, domain: String, callback: (Service) -> Unit) {
        dnssd.resolve(flags, ifIndex, serviceName, regType, domain, object : ResolveListener {
            override fun operationFailed(service: DNSSDService, errorCode: Int) {
                Timber.e("mDNS resolve failed (errorCode=$errorCode)")

            }

            override fun serviceResolved(
                resolver: DNSSDService,
                flags: Int,
                ifIndex: Int,
                fullName: String,
                hostName: String,
                port: Int,
                txtRecord: MutableMap<String, String>
            ) {
                resolver.stop()
                queryService(
                    flags = flags,
                    ifIndex = ifIndex,
                    serviceName = serviceName,
                    hostName = hostName,
                    port = port,
                    txtRecord = txtRecord,
                    callback = callback
                )
            }
        })
    }

    private fun queryService(
        flags: Int,
        ifIndex: Int,
        serviceName: String,
        hostName: String,
        port: Int,
        txtRecord: MutableMap<String, String>,
        callback: (Service) -> Unit
    ) {
        dnssd.queryRecord(flags, ifIndex, hostName, 1, 1, object : QueryListener {
            override fun operationFailed(service: DNSSDService, errorCode: Int) {
                Timber.e("mDNS query failed (errorCode=$errorCode)")
            }

            override fun queryAnswered(query: DNSSDService, flags: Int, ifIndex: Int, fullName: String, rrtype: Int, rrclass: Int, rdata: ByteArray, ttl: Int) {
                query.stop()
                val fixedHostname = hostName.removeSuffix(".")
                Timber.i("Resolved mDNS service $fixedHostname")

                // Construct OctoPrint
                val path = txtRecord["path"] ?: "/"
                val user = txtRecord["u"]
                val password = txtRecord["p"]
                val credentials = user?.let { u ->
                    password?.let { p -> "$u:$p@" } ?: "$u@"
                } ?: ""
                val device = Service(
                    label = serviceName,
                    hostname = fixedHostname,
                    port = port,
                    webUrl = "http://${credentials}${fixedHostname}:${port}$path",
                    host = InetAddress.getByAddress(hostName, rdata)
                )
                Injector.get().localDnsResolver().addMDnsDeviceToCache(device)
                callback(device)
            }
        })
    }

    data class Service(
        val label: String,
        val webUrl: String,
        val port: Int,
        val host: InetAddress,
        val hostname: String,
    )
}
