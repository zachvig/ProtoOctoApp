package de.crysxd.octoapp.base.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import de.crysxd.octoapp.base.di.Injector
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.IOException
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

class OctoPrintDnsSdDiscovery(
    context: Context,
) {
    private val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val nsd = context.applicationContext.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var bonjourResolveBusy = AtomicBoolean(false)
    private var bonjourResolveBacklog = mutableListOf<NsdServiceInfo>()

    fun discover(coroutineContext: CoroutineContext, callback: (Device) -> Unit) {
        val lock = wifi.createMulticastLock("OctoPrintUpnpDiscovery")

        try {
            lock.acquire()
            discoverWithMulticastLock(coroutineContext, callback)
        } catch (e: Exception) {
            Timber.e(e)
        } finally {
            lock.release()
        }
    }

    private fun discoverWithMulticastLock(coroutineContext: CoroutineContext, callback: (Device) -> Unit) {
        val discoverListener = NsdDiscoveryListener(
            onFound = { discoveredService ->
                Timber.i("Discovered service ${discoveredService.serviceName}")
                resolve(discoveredService, callback)
            },
            onError = {
                Timber.e(it, "Bonjour discover error")
            }
        )

        nsd.discoverServices("_octoprint._tcp", NsdManager.PROTOCOL_DNS_SD, discoverListener)
        coroutineContext.job.invokeOnCompletion {
            nsd.stopServiceDiscovery(discoverListener)
        }
    }

    private fun resolveFromBacklog(callback: (Device) -> Unit) {
        if (bonjourResolveBacklog.isNotEmpty()) {
            resolve(bonjourResolveBacklog.removeFirst(), callback)
        }
    }

    private fun resolve(service: NsdServiceInfo, callback: (Device) -> Unit): Job = GlobalScope.launch {
        // Gate
        if (!bonjourResolveBusy.compareAndSet(false, true)) {
            Timber.v("Bonjour resolve is busy, adding ${service.serviceName} to backlog")
            bonjourResolveBacklog.add(service)
            return@launch
        }
        Timber.i("Resolving service ${service.serviceName}")

        // Resolve
        nsd.resolveService(
            service,
            NsdResolveListener(
                onError = {
                    Timber.e(it, "Resolve error")
                    bonjourResolveBusy.set(false)
                    bonjourResolveBacklog.add(service)
                    resolveFromBacklog(callback)

                },
                onResolved = { resolvedService ->
                    bonjourResolveBusy.set(false)
                    resolveFromBacklog(callback)

                    Timber.i("Resolved service ${resolvedService.serviceName} (${resolvedService.host.hostAddress})")
                    // Construct OctoPrint
                    val path = resolvedService.attributes["path"]?.let { String(it) } ?: "/"
                    val user = resolvedService.attributes["u"]?.let { String(it) }
                    val password = resolvedService.attributes["p"]?.let { String(it) }
                    val credentials = user?.let { u ->
                        password?.let { p -> "$u:$p@" } ?: "$u@"
                    } ?: ""
                    val device = Device(
                        label = resolvedService.serviceName,
                        host = resolvedService.host,
                        port = resolvedService.port,
                        webUrl = "http://${credentials}${resolvedService.host.hostName}:${resolvedService.port}$path",
                    )
                    Injector.get().localDnsResolver().addMdnsDeviceToCache(device)
                    callback(device)
                }
            )
        )
    }

    private class NsdDiscoveryListener(
        private val onError: (Exception) -> Unit,
        private val onFound: (NsdServiceInfo) -> Unit,
    ) : NsdManager.DiscoveryListener {

        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            onError(IOException("Failed to start discovery ($serviceType $errorCode)"))
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            onError(IOException("Failed to stop discovery ($serviceType $errorCode)"))
        }

        override fun onDiscoveryStarted(serviceType: String) {
            Timber.i("Discovery started: $serviceType")
        }

        override fun onDiscoveryStopped(serviceType: String) {
            Timber.i("Discovery stopped: $serviceType")
        }

        override fun onServiceFound(service: NsdServiceInfo) {
            onFound(service)
        }

        override fun onServiceLost(service: NsdServiceInfo) = Unit
    }

    private class NsdResolveListener(
        private val onResolved: (NsdServiceInfo) -> Unit,
        private val onError: (Exception) -> Unit
    ) : NsdManager.ResolveListener {
        override fun onResolveFailed(service: NsdServiceInfo, errorCode: Int) {
            onError(IOException("Failed to resolve $service (errorCode=$errorCode)"))
        }

        override fun onServiceResolved(service: NsdServiceInfo) {
            onResolved(service)
        }
    }

    data class Device(
        val label: String,
        val webUrl: String,
        val port: Int,
        val host: InetAddress,
    )
}
