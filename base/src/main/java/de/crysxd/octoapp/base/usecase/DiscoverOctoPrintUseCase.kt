package de.crysxd.octoapp.base.usecase

import android.content.Context
import com.github.druk.dnssd.DNSSD
import de.crysxd.octoapp.base.logging.SensitiveDataMask
import de.crysxd.octoapp.base.network.OctoPrintDnsSdDiscovery
import de.crysxd.octoapp.base.network.OctoPrintUpnpDiscovery
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import timber.log.Timber
import java.net.InetAddress
import java.util.*
import javax.inject.Inject

class DiscoverOctoPrintUseCase @Inject constructor(
    private val context: Context,
    private val sensitiveDataMask: SensitiveDataMask,
    private val testFullNetworkStackUseCase: TestFullNetworkStackUseCase,
    private val dnssd: DNSSD,
) : UseCase<Unit, Flow<DiscoverOctoPrintUseCase.Result>>() {

    override suspend fun doExecute(param: Unit, timber: Timber.Tree): Flow<Result> = withContext(Dispatchers.IO) {
        val flow = MutableStateFlow(Result(emptyList()))
        val discoveredInstances = mutableListOf<DiscoveredOctoPrint>()
        val submitResult: (DiscoveredOctoPrint) -> Unit = { discovered ->
            if (!discoveredInstances.any { it.webUrl == discovered.webUrl }) {
                discoveredInstances.add(discovered)
                val uniqueDevices = discoveredInstances.groupBy { it.id }.values.mapNotNull {
                    it.maxByOrNull { i -> i.quality }
                }.sortedBy { it.label }
                flow.value = Result(discovered = uniqueDevices)
            }
        }

        // Start discovery and return results
        val coroutineJob = SupervisorJob()
        val coroutineScope = CoroutineScope(coroutineJob + Dispatchers.Main.immediate + CoroutineExceptionHandler { _, t -> timber.e(t) })
        return@withContext flow.onStart {
            timber.i("Starting OctoPrint discovery")
            discoverUsingDnsSd(timber, coroutineScope, submitResult)
            discoverUsingUpnp(timber, coroutineScope, submitResult)
        }.onCompletion {
            timber.i("Finishing OctoPrint discovery")
            coroutineJob.cancel()
            coroutineScope.cancel()
        }
    }

    private suspend fun discoverUsingDnsSd(
        timber: Timber.Tree,
        scope: CoroutineScope,
        submitResult: (DiscoveredOctoPrint) -> Unit
    ) = OctoPrintDnsSdDiscovery(context, dnssd).discover(scope) {
        scope.launch {
            timber.i("Testing $it")
            testDiscoveredInstanceAndPublishResult(
                timber = timber,
                instance = DiscoveredOctoPrint(
                    label = it.label,
                    detailLabel = it.hostname,
                    webUrl = it.webUrl.toHttpUrl(),
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
        scope: CoroutineScope,
        submitResult: (DiscoveredOctoPrint) -> Unit
    ) = scope.launch {
        val cache = mutableSetOf<String>()
        OctoPrintUpnpDiscovery(context, "Discover").discover {
            // Gate to eliminate duplicates
            if (cache.contains(it.upnpId)) return@discover
            cache.add(it.upnpId)

            scope.launch {
                timber.i("Testing $it")
                testDiscoveredInstanceAndPublishResult(
                    timber = timber,
                    instance = DiscoveredOctoPrint(
                        label = "OctoPrint via UPnP",
                        detailLabel = it.address.hostAddress,
                        webUrl = "http://${it.upnpHostname}:80/".toHttpUrl(),
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
        sensitiveDataMask.registerWebUrl(instance.webUrl)
        timber.i("Probing for '${instance.label}' at ${instance.webUrl} using ${instance.method} (${instance.id})")
        try {
            val result = testFullNetworkStackUseCase.execute(
                TestFullNetworkStackUseCase.Target.OctoPrint(
                    webUrl = instance.webUrl.toString(),
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
        val webUrl: HttpUrl,
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