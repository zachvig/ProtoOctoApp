package de.crysxd.octoapp.base.network

import android.content.Context
import com.github.druk.dnssd.BrowseListener
import com.github.druk.dnssd.DNSSDBindable
import com.github.druk.dnssd.DNSSDService
import com.github.druk.dnssd.QueryListener
import com.github.druk.dnssd.ResolveListener
import de.crysxd.octoapp.base.di.BaseInjector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import timber.log.Timber
import java.net.InetAddress


class OctoPrintDnsSdDiscovery(
    private val context: Context,
) {
    private val dnssd = DNSSDBindable(context)

    fun discover(scope: CoroutineScope, callback: (Service) -> Unit) {

        // Sometimes the internal Dnssd service is not running...we can start it with this:
        context.applicationContext.getSystemService(Context.NSD_SERVICE)

        try {
            scope.launch(Dispatchers.IO) {
                discoverWithMulticastLock(callback)
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    private suspend fun discoverWithMulticastLock(callback: (Service) -> Unit) {
        Timber.i("Starting mDNS discovery")
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

        currentCoroutineContext().job.invokeOnCompletion {
            Timber.i("Stopping mDNS discovery")
            service.stop()
        }

        Job().join()
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
                BaseInjector.get().localDnsResolver().addMDnsDeviceToCache(device)
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
