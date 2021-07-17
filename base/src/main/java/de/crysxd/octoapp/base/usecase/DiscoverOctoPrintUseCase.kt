package de.crysxd.octoapp.base.usecase

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.logging.SensitiveDataMask
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformationV2
import de.crysxd.octoapp.base.utils.UPnPDevice
import de.crysxd.octoapp.base.utils.UPnPDiscovery
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.io.IOException
import java.util.*
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

@Suppress("EXPERIMENTAL_API_USAGE")
class DiscoverOctoPrintUseCase @Inject constructor(
    private val context: Context,
    private val octoPrintProvider: OctoPrintProvider,
    private val sensitiveDataMask: SensitiveDataMask,
) : UseCase<Unit, Flow<DiscoverOctoPrintUseCase.Result>>() {

    private val manager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var bonjourResolveBusy = false
    private var bonjourResolveBacklog = mutableListOf<NsdServiceInfo>()

    override suspend fun doExecute(param: Unit, timber: Timber.Tree): Flow<Result> = withContext(Dispatchers.IO) {
        val channel = ConflatedBroadcastChannel(Result(emptyList()))
        val discoveredInstances = mutableListOf<DiscoveredOctoPrint>()
        val submitResult: (DiscoveredOctoPrint) -> Unit = { discovered ->
            if (!discoveredInstances.any { it.webUrl == discovered.webUrl }) {
                discoveredInstances.add(discovered)
                val uniqueDevices = discoveredInstances.groupBy { it.id }.values.mapNotNull {
                    it.maxByOrNull { i -> i.quality }
                }.sortedBy { it.label }
                channel.offer(Result(discovered = uniqueDevices))
            }
        }

        // Pixel devices need a multicast lock to find any Bonjour services
        val wifiManager = (context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager)

        // Start discovery and return results
        var multicastLock: WifiManager.MulticastLock? = null
        var upnpDiscovery: UPnPDiscovery? = null
        var bonjourListener: NsdDiscoveryListener? = null
        return@withContext channel.asFlow().onStart {
            timber.i("Starting Bonjour discovery")
            multicastLock = wifiManager.createMulticastLock("octoprint-discovery")
            multicastLock?.setReferenceCounted(true)
            multicastLock?.acquire()
            bonjourListener = discoverUsingBonjour(timber, currentCoroutineContext(), submitResult)
            upnpDiscovery = discoverUsingUpnp(timber, currentCoroutineContext(), submitResult)
        }.onCompletion {
            timber.i("Finishing Bonjour discovery")
            bonjourListener?.let(manager::stopServiceDiscovery)
            multicastLock?.release()
            upnpDiscovery?.cancel(true)
        }
    }

    private fun discoverUsingBonjour(
        timber: Timber.Tree,
        coroutineContext: CoroutineContext,
        submitResult: (DiscoveredOctoPrint) -> Unit
    ): NsdDiscoveryListener {
        val discoverListener = NsdDiscoveryListener(
            timber = timber,
            onFound = { discoveredService ->
                timber.i("Discovered service ${discoveredService.serviceName}")
                resolveBonjourService(timber, coroutineContext, discoveredService, submitResult)
            },
            onError = {
                timber.e(it, "Bonjour discover error")
            }
        )
        manager.discoverServices("_octoprint._tcp", NsdManager.PROTOCOL_DNS_SD, discoverListener)
        return discoverListener
    }

    private fun discoverUsingUpnp(
        timber: Timber.Tree,
        coroutineContext: CoroutineContext,
        submitResult: (DiscoveredOctoPrint) -> Unit
    ): UPnPDiscovery {
        val discoveryCache = mutableListOf<String>()
        return UPnPDiscovery(
            context,
            UpnpDiscoveryListener(
                timber = timber,
                onError = { timber.e(it, "Upnp error") },
                onFound = {
                    if (discoveryCache.contains(it.hostAddress)) return@UpnpDiscoveryListener

                    discoveryCache.add(it.hostAddress)
                    timber.i("Upnp device found: ${it.hostAddress}")
                    GlobalScope.launch(coroutineContext + Dispatchers.IO) {
                        testDiscoveredInstanceAndPublishResult(
                            timber = timber,
                            instance = DiscoveredOctoPrint(
                                label = "OctoPrint on ${it.hostAddress}",
                                detailLabel = "http://${it.hostAddress}:80/",
                                webUrl = "http://${it.hostAddress}:80/",
                                id = "${it.hostAddress}:80".hashCode(),
                                bonjourServiceName = null,
                                bonjourServiceType = null,
                                source = "Upnp",
                                quality = 0,
                            ),
                            submitResult = submitResult
                        )
                    }
                }
            )
        ).also {
            it.execute()
        }
    }

    private fun resolveBonjourServiceFromBacklog(timber: Timber.Tree, coroutineContext: CoroutineContext, submitResult: (DiscoveredOctoPrint) -> Unit) {
        bonjourResolveBacklog.firstOrNull()?.let {
            bonjourResolveBacklog.remove(it)
            timber.i("Resolving ${it.serviceName} from backlog")
            resolveBonjourService(timber, coroutineContext, it, submitResult)
        }
    }

    private fun resolveBonjourService(timber: Timber.Tree, coroutineContext: CoroutineContext, service: NsdServiceInfo, submitResult: (DiscoveredOctoPrint) -> Unit) {
        if (bonjourResolveBusy) {
            timber.i("Bonjour resolve is busy, adding ${service.serviceName} to backlog")
            bonjourResolveBacklog.add(service)
            return
        }
        timber.i("Resolving service ${service.serviceName}")

        bonjourResolveBusy = true

        manager.resolveService(
            service,
            NsdResolveListener(
                onError = {
                    timber.e(it, "Resolve error")
                    bonjourResolveBusy = false
                    bonjourResolveBacklog.add(service)

                    GlobalScope.launch(coroutineContext) {
                        delay(500L)
                        resolveBonjourServiceFromBacklog(timber, coroutineContext, submitResult)
                    }
                },
                onResolved = { resolvedService ->
                    bonjourResolveBusy = false

                    GlobalScope.launch(coroutineContext) {
                        resolveBonjourServiceFromBacklog(timber, coroutineContext, submitResult)
                    }

                    Timber.i("Resolved service ${resolvedService.serviceName}")
                    // Construct OctoPrint
                    val path = resolvedService.attributes["path"]?.let { String(it) } ?: "/"
                    val user = resolvedService.attributes["u"]?.let { String(it) }
                    val password = resolvedService.attributes["p"]?.let { String(it) }
                    val credentials = user?.let { u ->
                        password?.let { p -> "$u:$p@" } ?: "$u@"
                    } ?: ""
                    val maskedCredentials = user?.let { _ ->
                        password?.let { _ -> "***:***@" } ?: "***@"
                    } ?: ""

                    GlobalScope.launch(coroutineContext + Dispatchers.IO) {
                        testDiscoveredInstanceAndPublishResult(
                            timber = timber,
                            submitResult = submitResult,
                            instance = DiscoveredOctoPrint(
                                label = resolvedService.serviceName,
                                detailLabel = "http://${maskedCredentials}${resolvedService.host.hostName}:${resolvedService.port}$path",
                                webUrl = "http://${credentials}${resolvedService.host.hostName}:${resolvedService.port}$path",
                                id = "${resolvedService.host.hostAddress}:${resolvedService.port}".hashCode(),
                                bonjourServiceName = resolvedService.serviceName,
                                bonjourServiceType = resolvedService.serviceType,
                                source = "Bonjour",
                                quality = 100,
                            )
                        )
                    }
                }
            )
        )
    }

    private suspend fun testDiscoveredInstanceAndPublishResult(timber: Timber.Tree, instance: DiscoveredOctoPrint, submitResult: (DiscoveredOctoPrint) -> Unit) {
        sensitiveDataMask.registerWebUrl(instance.webUrl, instance.bonjourServiceName ?: "octoprint_from_bonjour")
        timber.i("Probing for '${instance.label}' at ${instance.webUrl} using ${instance.source} (${instance.id})")
        val octoPrint = octoPrintProvider.createAdHocOctoPrint(OctoPrintInstanceInformationV2(webUrl = instance.webUrl, apiKey = ""))
        try {
            octoPrint.createUserApi().getCurrentUser().isGuest
            timber.i("Probe for '${instance.label}' at ${instance.webUrl} was SUCCESS 🥳")
            submitResult(instance)
        } catch (e: java.lang.Exception) {
            timber.i("Probe for '${instance.label}' at ${instance.webUrl} was FAILURE 😭 (${e.message})")
        }
    }

    data class Result(
        val discovered: List<DiscoveredOctoPrint>,
    )

    data class DiscoveredOctoPrint(
        val label: String,
        val detailLabel: String,
        val webUrl: String,
        val id: Int,
        val bonjourServiceName: String?,
        val bonjourServiceType: String?,
        val source: String,
        val quality: Int,
    )

    private class NsdDiscoveryListener(
        private val timber: Timber.Tree,
        private val onError: (Exception) -> Unit,
        private val onFound: (NsdServiceInfo) -> Unit,
        private val onLost: (NsdServiceInfo) -> Unit = {}
    ) : NsdManager.DiscoveryListener {

        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            timber.e("Discovery failed: $serviceType $errorCode")
            onError(IOException("Failed to start discovery ($serviceType $errorCode)"))
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            timber.e("Discovery stop failed: $serviceType $errorCode")
            onError(IOException("Failed to stop discovery ($serviceType $errorCode)"))
        }

        override fun onDiscoveryStarted(serviceType: String) {
            timber.i("Discovery started: $serviceType")
        }

        override fun onDiscoveryStopped(serviceType: String) {
            timber.i("Discovery stopped: $serviceType")
        }

        override fun onServiceFound(service: NsdServiceInfo) {
            timber.i("Service found: $service")
            onFound(service)

        }

        override fun onServiceLost(service: NsdServiceInfo) {
            timber.i("Service lost: $service")
            onLost(service)
        }
    }

    private class UpnpDiscoveryListener(
        private val timber: Timber.Tree,
        private val onFound: (UPnPDevice) -> Unit,
        private val onError: (Exception) -> Unit
    ) : UPnPDiscovery.OnDiscoveryListener {
        override fun OnStart() {
            timber.i("UPnp discovery started")
        }

        override fun OnFoundNewDevice(device: UPnPDevice) {
            onFound(device)
        }

        override fun OnFinish() {
            timber.i("UPnp discovery finished")
        }

        override fun OnError(e: java.lang.Exception) {
            onError(e)
        }
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
}