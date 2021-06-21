package de.crysxd.octoapp.base.usecase

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformationV2
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.onCompletion
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class DiscoverOctoPrintUseCase @Inject constructor(
    private val context: Context,
    private val octoPrintProvider: OctoPrintProvider
) : UseCase<Unit, Flow<DiscoverOctoPrintUseCase.Result>>() {

    private val manager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var bonjourResolveBusy = false
    private var bonjourResolveBacklog = mutableListOf<NsdServiceInfo>()

    override suspend fun doExecute(param: Unit, timber: Timber.Tree): Flow<Result> = withContext(Dispatchers.IO) {
        val channel = ConflatedBroadcastChannel(Result(emptyList()))
        val discoveredInstances = mutableListOf<DiscoveredOctoPrint>()
        val coroutineContext = Job()
        val submitResult: (DiscoveredOctoPrint) -> Unit = { discovered ->
            if (!discoveredInstances.any { it.webUrl == discovered.webUrl }) {
                discoveredInstances.add(discovered)
                channel.offer(Result(discovered = discoveredInstances))
            }
        }

        val bonjourListener = discoverUsingBonjour(timber, coroutineContext, submitResult)
        discoverUsingUPnP(timber, coroutineContext, submitResult)

        return@withContext channel.asFlow().onCompletion {
            coroutineContext.cancel()
            timber.i("Finishing Bonjour discovery")
            manager.stopServiceDiscovery(bonjourListener)
        }
    }

    private fun discoverUsingUPnP(timber: Timber.Tree, coroutineContext: CoroutineContext, submitResult: (DiscoveredOctoPrint) -> Unit) {
        timber.i("Preparing UPnP discovery")

    }

    private fun discoverUsingBonjour(
        timber: Timber.Tree,
        coroutineContext: CoroutineContext,
        submitResult: (DiscoveredOctoPrint) -> Unit
    ): NsdDiscoveryListener {
        timber.i("Preparing Bonjour discovery")

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
                        delay(2000L)
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
                    val maskedCredentials = user?.let { u ->
                        password?.let { p -> "***:***@" } ?: "***@"
                    } ?: ""

                    GlobalScope.launch(coroutineContext) {
                        testDiscoveredInstanceAndPublishResult(
                            timber = timber,
                            submitResult = submitResult,
                            instance = DiscoveredOctoPrint(
                                label = resolvedService.serviceName,
                                detailLabel = "http://${maskedCredentials}${resolvedService.host.hostName}:${resolvedService.port}$path",
                                webUrl = "http://${credentials}${resolvedService.host.hostAddress}:${resolvedService.port}$path",
                                bonjourServiceName = resolvedService.serviceName,
                                bonjourServiceType = resolvedService.serviceType,
                                source = "Bonjour"
                            )
                        )
                    }
                }
            )
        )
    }

    private suspend fun testDiscoveredInstanceAndPublishResult(timber: Timber.Tree, instance: DiscoveredOctoPrint, submitResult: (DiscoveredOctoPrint) -> Unit) {
        timber.i("Probing resolved instance at ${instance.webUrl} using ${instance.source}")
        val octoPrint = octoPrintProvider.createAdHocOctoPrint(OctoPrintInstanceInformationV2(webUrl = instance.webUrl, apiKey = ""))
        if (octoPrint.createApplicationKeysPluginApi().probe()) {
            timber.i("Probe for ${instance.label} at ${instance.webUrl} was SUCCESS 🥳")
            submitResult(instance)
        } else {
            timber.i("Probe for ${instance.label} at ${instance.webUrl} was FAILURE 😭")
        }
    }

    data class Result(
        val discovered: List<DiscoveredOctoPrint>,
    )

    data class DiscoveredOctoPrint(
        val label: String,
        val detailLabel: String,
        val webUrl: String,
        val bonjourServiceName: String?,
        val bonjourServiceType: String?,
        val source: String,
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