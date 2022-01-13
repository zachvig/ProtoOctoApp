package de.crysxd.octoapp.base.network

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import de.crysxd.octoapp.base.BuildConfig
import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.base.OctoPreferences
import de.crysxd.octoapp.base.data.models.OctoPrintInstanceInformationV3
import de.crysxd.octoapp.base.data.models.exceptions.SuppressedIllegalStateException
import de.crysxd.octoapp.base.data.repository.OctoPrintRepository
import de.crysxd.octoapp.base.di.BaseInjector
import de.crysxd.octoapp.base.logging.TimberLogger
import de.crysxd.octoapp.base.utils.AppScope
import de.crysxd.octoapp.octoprint.OctoPrint
import de.crysxd.octoapp.octoprint.SubjectAlternativeNameCompatVerifier
import de.crysxd.octoapp.octoprint.exceptions.RemoteServiceConnectionBrokenException
import de.crysxd.octoapp.octoprint.models.socket.Event
import de.crysxd.octoapp.octoprint.models.socket.Message
import de.crysxd.octoapp.octoprint.websocket.EventFlowConfiguration
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

@Suppress("EXPERIMENTAL_API_USAGE")
class OctoPrintProvider(
    private val detectBrokenSetupInterceptor: DetectBrokenSetupInterceptor,
    private val octoPrintRepository: OctoPrintRepository,
    private val octoPreferences: OctoPreferences,
    private val analytics: FirebaseAnalytics,
    private val sslKeyStoreHandler: SslKeyStoreHandler,
    private val localDnsResolver: LocalDnsResolver,
) {

    private val octoPrintMutex = Mutex()
    private var octoPrintCache = mutableMapOf<String, Pair<String, OctoPrint>>()
    private val currentMessageFlow = mutableMapOf<String, MutableStateFlow<Message.CurrentMessage?>>()
    private val companionMessageFlow = mutableMapOf<String, MutableStateFlow<Message.CompanionPluginMessage?>>()
    private val connectEventFlow = mutableMapOf<String, MutableStateFlow<Event.Connected?>>()
    private val activeInstanceId get() = octoPrintRepository.getActiveInstanceSnapshot()?.id

    init {
        // Passively collect data for the analytics profile
        // The passive event flow does not actively open a connection but piggy-backs other Flows
        AppScope.launch {
            octoPrintRepository.instanceInformationFlow().mapNotNull { it?.id }.flatMapLatest { instanceId ->
                passiveEventFlow(instanceId).onEach { event ->
                    updateAnalyticsProfileWithEvents(event)
                    (event as? Event.MessageReceived)?.let {
                        when (val message = (event as? Event.MessageReceived)?.message) {
                            is Message.CurrentMessage -> {
                                // If the last message had data the new one is lacking, upgrade the new one so the cached message
                                // is always holding all information required
                                val flow = createCurrentMessageFlow(instanceId)
                                val last = flow.value
                                val new = message.copy(
                                    temps = message.temps.takeIf { it.isNotEmpty() } ?: last?.temps ?: emptyList(),
                                    progress = message.progress ?: last?.progress,
                                    state = message.state ?: last?.state,
                                    job = message.job ?: last?.job,
                                )
                                flow.value = new
                            }

                            is Message.CompanionPluginMessage -> {
                                createCompanionMessageFlow(instanceId).value = message
                            }

                            else -> Unit
                        }
                    }

                    ((event as? Event.Connected))?.let {
                        createConnectEventFlow(instanceId).value = it
                    }

                    ((event as? Event.Disconnected))?.let {
                        createConnectEventFlow(instanceId).value = null
                        it.exception?.let(detectBrokenSetupInterceptor::handleException)
                    }
                }
            }.retry { delay(1000); true }.collect()
        }
    }

    private val OctoPrintInstanceInformationV3?.cacheKey get() = this?.apiKey + this?.webUrl + this?.alternativeWebUrl

    fun octoPrintFlow(instanceId: String? = null) = octoPrintRepository.instanceInformationFlow(instanceId).distinctUntilChangedBy {
        it.cacheKey
    }.map { instance ->
        octoPrintMutex.withLock {
            val cached = octoPrintCache[instance?.id]

            when {
                // We don't have information for the instance, clean up
                // Use instanceId not instance.id!
                instance == null -> {
                    Timber.d("Instance is null, clearing cached passive flows")
                    instanceId?.let {
                        createConnectEventFlow(it).tryEmit(null)
                        createCurrentMessageFlow(it).tryEmit(null)
                    }
                    null
                }

                // Use instance.id to ensure handover when active changes!
                cached == null || cached.first != instance.cacheKey -> {
                    val octoPrint = createAdHocOctoPrint(instance)
                    Timber.d("Created new OctoPrint: $octoPrint")
                    octoPrintCache[instance.id] = instance.cacheKey to octoPrint
                    octoPrint
                }

                else -> {
                    Timber.d("Took OctoPrint from cache: ${cached.second}")
                    cached.second
                }
            }
        }
    }

    suspend fun octoPrint(instanceId: String? = null): OctoPrint = octoPrintMutex.withLock {
        val id = instanceId ?: octoPrintRepository.getActiveInstanceSnapshot()?.id
        octoPrintCache[id]?.second ?: throw SuppressedIllegalStateException("OctoPrint not available")
    }

    val currentConnection = activeInstanceId?.let { createConnectEventFlow(it).value }

    fun passiveConnectionEventFlow(tag: String, instanceId: String? = null) = instanceIdFlow(instanceId)
        .flatMapLatest { it?.let { createConnectEventFlow(it) } ?: emptyFlow() }
        .filterNotNull()
        .onStart { Timber.i("Started connection event flow for $tag on instance $instanceId") }
        .onCompletion { Timber.i("Completed connection event flow for $tag on instance $instanceId") }

    fun passiveCurrentMessageFlow(tag: String, instanceId: String? = null) = instanceIdFlow(instanceId)
        .flatMapLatest { it?.let { createCurrentMessageFlow(it) } ?: emptyFlow() }
        .filterNotNull()
        .onStart { Timber.i("Started current message flow for $tag on instance $instanceId") }
        .onCompletion { Timber.i("Completed current message flow for $tag on instance $instanceId") }

    fun passiveCompanionMessageFlow(tag: String, instanceId: String? = null) = instanceIdFlow(instanceId)
        .flatMapLatest { it?.let { createCompanionMessageFlow(it) } ?: emptyFlow() }
        .onStart { Timber.i("Started companion message flow for $tag on instance $instanceId") }
        .onCompletion { Timber.i("Completed companion message flow for $tag on instance $instanceId ") }

    fun eventFlow(tag: String, instanceId: String? = null, config: EventFlowConfiguration = EventFlowConfiguration()) = octoPrintFlow(instanceId)
        .flatMapLatest { it?.getEventWebSocket()?.eventFlow(tag, config) ?: emptyFlow() }
        .catch { e -> Timber.e(e) }
        .onStart { Timber.i("Started event flow for $tag on instance $instanceId") }
        .onCompletion { Timber.i("Completed event message flow for $tag on instance $instanceId ") }

    fun passiveEventFlow(instanceId: String? = null) = octoPrintFlow(instanceId)
        .flatMapLatest { it?.getEventWebSocket()?.passiveEventFlow() ?: emptyFlow() }
        .retry { e ->
            Timber.e(e)
            delay(1000)
            true
        }

    private fun instanceIdFlow(instanceId: String?) =
        (instanceId?.let { flowOf(instanceId) } ?: octoPrintRepository.instanceInformationFlow().map { it?.id }).distinctUntilChanged()

    private fun createCurrentMessageFlow(instanceId: String) = currentMessageFlow.getOrPut(instanceId) { MutableStateFlow(null) }

    private fun createConnectEventFlow(instanceId: String) = connectEventFlow.getOrPut(instanceId) { MutableStateFlow(null) }

    private fun createCompanionMessageFlow(instanceId: String) = companionMessageFlow.getOrPut(instanceId) { MutableStateFlow(null) }

    private fun updateAnalyticsProfileWithEvents(event: Event) {
        when {
            event is Event.MessageReceived && event.message is Message.EventMessage.FirmwareData -> {
                val data = event.message as Message.EventMessage.FirmwareData
                analytics.logEvent("printer_firmware_data") {
                    param("firmware_name", data.firmwareName ?: "unspecified")
                    param("machine_type", data.machineType ?: "unspecified")
                    param("extruder_count", data.extruderCount?.toLong() ?: 0)
                }
                analytics.setUserProperty("printer_firmware_name", data.firmwareName)
                analytics.setUserProperty("printer_machine_type", data.machineType)
                analytics.setUserProperty("printer_extruder_count", data.extruderCount.toString())
            }

            event is Event.MessageReceived && event.message is Message.ConnectedMessage -> {
                OctoAnalytics.logEvent(OctoAnalytics.Event.OctoprintConnected)
                OctoAnalytics.setUserProperty(OctoAnalytics.UserProperty.OctoPrintVersion, (event.message as Message.ConnectedMessage).version)
            }
        }
    }

    private fun handleNetworkException(e: Exception) = AppScope.launch {
        when (e) {
            // The OE connection is broken, remove. User will be informed by regular error dialog
            is RemoteServiceConnectionBrokenException -> {
                Timber.w("Caught OctoEverywhere/SpaghettiDetective exception, removing connection")
                BaseInjector.get().handleRemoteServiceConnectionBrokenException().execute(e)
            }

            else -> Unit
        }
    }

    fun createAdHocOctoPrint(it: OctoPrintInstanceInformationV3) = OctoPrint(
        id = it.id,
        rawWebUrl = it.webUrl,
        rawAlternativeWebUrl = it.alternativeWebUrl,
        apiKey = it.apiKey,
        highLevelInterceptors = listOf(detectBrokenSetupInterceptor),
        customDns = localDnsResolver,
        keyStore = sslKeyStoreHandler.loadKeyStore(),
        hostnameVerifier = SubjectAlternativeNameCompatVerifier().takeIf { _ -> sslKeyStoreHandler.isWeakVerificationForHost(it.webUrl) },
        networkExceptionListener = ::handleNetworkException,
        connectTimeoutMs = Firebase.remoteConfig.getLong("connection_timeout_ms"),
        readWriteTimeout = Firebase.remoteConfig.getLong("read_write_timeout_ms"),
        webSocketConnectionTimeout = Firebase.remoteConfig.getLong("web_socket_connect_timeout_ms"),
        webSocketPingPongTimeout = Firebase.remoteConfig.getLong("web_socket_ping_pong_timeout_ms"),
        debug = BuildConfig.DEBUG,
        httpEventListener = NetworkDebugLogger().takeIf { octoPreferences.debugNetworkLogging }
    ).also { octoPrint ->
        // Setup logger to use timber
        TimberLogger(octoPrint.getLogger())
    }
}
