package de.crysxd.octoapp.base.usecase

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.onCompletion
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

class DiscoverOctoPrintUseCase @Inject constructor(private val context: Context) : UseCase<Unit, Flow<DiscoverOctoPrintUseCase.Result>>() {

    override suspend fun doExecute(param: Unit, timber: Timber.Tree): Flow<Result> = withContext(Dispatchers.IO) {
        val channel = ConflatedBroadcastChannel(Result(emptyList()))
        val manager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
        var error: Exception? = null
        val discoveredInstances = mutableListOf<DiscoveredOctoPrint>()

        fun resolveService(service: NsdServiceInfo) {
            manager.resolveService(
                service,
                NsdResolveListener(
                    timber = timber,
                    onError = {
                        timber.e(it, "Resolve error")
                        error = it
                        resolveService(service)
                    },
                    onResolved = { resolvedService ->
                        Timber.i("Resolved service ${resolvedService.serviceName}")
                        // Construct OctoPrint
                        val path = resolvedService.attributes["path"]?.let { String(it) } ?: "/"
                        val user = resolvedService.attributes["u"]
                        val password = resolvedService.attributes["p"]
                        val credentials = user?.let { u ->
                            password?.let { p -> "$u:$p@" } ?: "$u@"
                        } ?: ""
                        val maskedCredentials = user?.let { u ->
                            password?.let { p -> "***:***@" } ?: "***@"
                        } ?: ""
                        discoveredInstances.add(
                            DiscoveredOctoPrint(
                                label = resolvedService.serviceName,
                                detailLabel = "http://${maskedCredentials}${resolvedService.host.hostAddress}:${resolvedService.port}$path",
                                webUrl = "http://${credentials}${resolvedService.host.hostAddress}:${resolvedService.port}$path",
                                zeroConfServiceName = resolvedService.serviceName,
                                zeroConfServiceType = resolvedService.serviceType,
                                order = discoveredInstances.size,
                            )
                        )

                        // Send new result
                        channel.offer(
                            Result(
                                error = error,
                                discovered = discoveredInstances.distinctBy { it.webUrl }.sortedBy { it.order }
                            )
                        )
                    }
                )
            )
        }

        val discoverListener = NsdDiscoveryListener(
            timber = timber,
            onFound = { discoveredService ->
                Timber.i("Discovered service ${discoveredService.serviceName}")
                resolveService(discoveredService)
            },
            onError = {
                timber.e(it, "Dsicover error")
                error = it
            }
        )
        timber.i("Preparing DNS SD discovery")
        manager.discoverServices("_octoprint._tcp", NsdManager.PROTOCOL_DNS_SD, discoverListener)
        return@withContext channel.asFlow().onCompletion {
            manager.stopServiceDiscovery(discoverListener)
        }
    }

    data class Result(
        val discovered: List<DiscoveredOctoPrint>,
        val error: Exception? = null
    )

    data class DiscoveredOctoPrint(
        val label: String,
        val detailLabel: String,
        val webUrl: String,
        val zeroConfServiceName: String,
        val zeroConfServiceType: String,
        val order: Int
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
        private val timber: Timber.Tree,
        private val onResolved: (NsdServiceInfo) -> Unit,
        private val onError: (Exception) -> Unit
    ) : NsdManager.ResolveListener {
        override fun onResolveFailed(service: NsdServiceInfo, errorCode: Int) {
            onError(IOException("Failed to resolve $service ($errorCode)"))
        }

        override fun onServiceResolved(service: NsdServiceInfo) {
            onResolved(service)
        }
    }

    data class OctoPrintService(
        val serviceName: String,
    )
}