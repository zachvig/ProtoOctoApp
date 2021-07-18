package de.crysxd.octoapp.base.usecase

import android.content.Context
import de.crysxd.octoapp.base.logging.SensitiveDataMask
import de.crysxd.octoapp.base.network.OctoPrintDnsSdDiscovery
import de.crysxd.octoapp.base.network.OctoPrintUpnpDiscovery
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.net.InetAddress
import java.util.*
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

@Suppress("EXPERIMENTAL_API_USAGE")
class DiscoverOctoPrintUseCase @Inject constructor(
    private val context: Context,
    private val sensitiveDataMask: SensitiveDataMask,
    private val testFullNetworkStackUseCase: TestFullNetworkStackUseCase,
) : UseCase<Unit, Flow<DiscoverOctoPrintUseCase.Result>>() {

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

        // Start discovery and return results
        var job: Job? = null
        return@withContext channel.asFlow().onStart {
            timber.i("Starting OctoPrint discovery")
            job = Job()
            discoverUsingDnsSd(timber, currentCoroutineContext(), submitResult)
            discoverUsingUpnp(timber, currentCoroutineContext(), submitResult)
        }.onCompletion {
            timber.i("Finishing OctoPrint discovery")
            job?.cancel()
        }
    }

    private suspend fun discoverUsingDnsSd(
        timber: Timber.Tree,
        coroutineContext: CoroutineContext,
        submitResult: (DiscoveredOctoPrint) -> Unit
    ) = OctoPrintDnsSdDiscovery(context).discover(coroutineContext) {
        GlobalScope.launch(coroutineContext) {
            timber.i("Testing $it")
            testDiscoveredInstanceAndPublishResult(
                timber = timber,
                instance = DiscoveredOctoPrint(
                    label = it.label,
                    detailLabel = it.hostname,
                    webUrl = it.webUrl,
                    host = it.host,
                    method = DiscoveryMethod.DnsSd,
                    quality = 100,
                    port = it.port,
                ),
                submitResult = submitResult
            )
        }
    }

    private suspend fun discoverUsingUpnp(
        timber: Timber.Tree,
        coroutineContext: CoroutineContext,
        submitResult: (DiscoveredOctoPrint) -> Unit
    ) = GlobalScope.launch(coroutineContext) {
        val cache = mutableSetOf<String>()
        OctoPrintUpnpDiscovery(context).discover {
            // Gate to eliminate duplicates
            if (cache.contains(it.upnpId)) return@discover
            cache.add(it.upnpId)

            GlobalScope.launch(coroutineContext) {
                timber.i("Testing $it")
                testDiscoveredInstanceAndPublishResult(
                    timber = timber,
                    instance = DiscoveredOctoPrint(
                        label = "OctoPrint via UPnP",
                        detailLabel = it.address.hostAddress,
                        webUrl = "http://${it.upnpHostname}:80/",
                        port = 80,
                        host = it.address,
                        method = DiscoveryMethod.Upnp,
                        quality = 0,
                    ),
                    submitResult = submitResult
                )
            }
        }
    }

    private suspend fun testDiscoveredInstanceAndPublishResult(timber: Timber.Tree, instance: DiscoveredOctoPrint, submitResult: (DiscoveredOctoPrint) -> Unit) {
        sensitiveDataMask.registerWebUrl(instance.webUrl, "octoprint_from_${instance.method::class.java.simpleName.lowercase()}")
        timber.i("Probing for '${instance.label}' at ${instance.webUrl} using ${instance.method} (${instance.id})")
        try {
            val result = testFullNetworkStackUseCase.execute(
                TestFullNetworkStackUseCase.Target.OctoPrint(
                    webUrl = instance.webUrl,
                    apiKey = ""
                )
            )

            when (result) {
                is TestFullNetworkStackUseCase.Finding.OctoPrintReady,
                is TestFullNetworkStackUseCase.Finding.InvalidApiKey -> {
                    timber.i("Probe for '${instance.label}' at ${instance.webUrl} was SUCCESS 🥳")
                    submitResult(instance)
                }
                else -> timber.i("Probe for '${instance.label}' at ${instance.webUrl} was FAILURE 😭 (finding=$result)")
            }
        } catch (e: java.lang.Exception) {
            timber.i("Probe for '${instance.label}' at ${instance.webUrl} was FAILURE because of an error 😭 (error=${e.message})")
        }
    }

    data class Result(
        val discovered: List<DiscoveredOctoPrint>,
    )

    data class DiscoveredOctoPrint(
        val label: String,
        val detailLabel: String,
        val webUrl: String,
        val port: Int,
        val host: InetAddress,
        val method: DiscoveryMethod,
        val quality: Int,
    ) {
        val id get() = "${host.hostAddress}:$port".hashCode()
    }

    sealed class DiscoveryMethod {
        object DnsSd : DiscoveryMethod()
        object Upnp : DiscoveryMethod()
    }
}