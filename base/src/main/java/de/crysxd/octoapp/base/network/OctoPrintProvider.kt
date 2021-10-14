package de.crysxd.octoapp.base.network

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import de.crysxd.octoapp.base.BuildConfig
import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.base.data.models.OctoPrintInstanceInformationV3
import de.crysxd.octoapp.base.data.repository.OctoPrintRepository
import de.crysxd.octoapp.base.di.BaseInjector
import de.crysxd.octoapp.base.logging.TimberLogger
import de.crysxd.octoapp.base.utils.AppScope
import de.crysxd.octoapp.octoprint.OctoPrint
import de.crysxd.octoapp.octoprint.SubjectAlternativeNameCompatVerifier
import de.crysxd.octoapp.octoprint.exceptions.OctoEverywhereConnectionNotFoundException
import de.crysxd.octoapp.octoprint.exceptions.OctoEverywhereSubscriptionMissingException
import de.crysxd.octoapp.octoprint.models.socket.Event
import de.crysxd.octoapp.octoprint.models.socket.Message
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
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
    private val analytics: FirebaseAnalytics,
    private val sslKeyStoreHandler: SslKeyStoreHandler,
    private val localDnsResolver: LocalDnsResolver,
) {

    private val octoPrintMutex = Mutex()
    private var octoPrintCache: Pair<OctoPrintInstanceInformationV3, OctoPrint>? = null
    private val currentMessageFlow = MutableStateFlow<Message.CurrentMessage?>(null)
    private val connectEventFlow = MutableStateFlow<Event.Connected?>(null)

    init {
        // Passively collect data for the analytics profile
        // The passive event flow does not actively open a connection but piggy-backs other Flows
        AppScope.launch {
            passiveEventFlow().onEach { event ->
                updateAnalyticsProfileWithEvents(event)
                ((event as? Event.MessageReceived)?.message as? Message.CurrentMessage)?.let { message ->
                    // If the last message had data the new one is lacking, upgrade the new one so the cached message
                    // is always holding all information required
                    val last = currentMessageFlow.value
                    val new = message.copy(
                        temps = message.temps.takeIf { it.isNotEmpty() } ?: last?.temps ?: emptyList(),
                        progress = message.progress ?: last?.progress,
                        state = message.state ?: last?.state,
                        job = message.job ?: last?.job,
                    )
                    currentMessageFlow.value = new
                }

                ((event as? Event.Connected))?.let {
                    connectEventFlow.value = it
                }

                ((event as? Event.Disconnected))?.let {
                    connectEventFlow.value = null
                    it.exception?.let(detectBrokenSetupInterceptor::handleException)
                }
            }.retry { delay(1000); true }.collect()
        }
    }

    private val OctoPrintInstanceInformationV3?.cacheKey get() = this?.apiKey + this?.webUrl + this?.alternativeWebUrl

    fun octoPrintFlow() = octoPrintRepository.instanceInformationFlow().distinctUntilChangedBy {
        it.cacheKey
    }.map {
        octoPrintMutex.withLock {
            when {
                it == null -> {
                    Timber.d("Instance is null, clearing cached passive flows")
                    currentMessageFlow.tryEmit(null)
                    connectEventFlow.tryEmit(null)
                    null
                }
                octoPrintCache?.first.cacheKey != it.cacheKey -> {
                    val octoPrint = createAdHocOctoPrint(it)
                    Timber.d("Created new OctoPrint: $octoPrint")
                    octoPrintCache = Pair(it, octoPrint)
                    octoPrint
                }
                else -> {
                    Timber.d("Took OctoPrint from cache: ${octoPrintCache?.second}")
                    octoPrintCache?.second
                }
            }
        }
    }

    suspend fun octoPrint(): OctoPrint = octoPrintMutex.withLock {
        octoPrintCache?.second ?: throw IllegalStateException("OctoPrint not available")
    }

    fun passiveConnectionEventFlow(tag: String) = connectEventFlow.filterNotNull()
        .onStart { Timber.i("Started connection event flow for $tag") }
        .onCompletion { Timber.i("Completed connection event flow for $tag") }

    fun passiveEventFlow() = octoPrintFlow()
        .flatMapLatest { it?.getEventWebSocket()?.passiveEventFlow() ?: emptyFlow() }
        .retry { e ->
            Timber.e(e)
            delay(1000)
            true
        }

    fun passiveCurrentMessageFlow(tag: String) = currentMessageFlow.filterNotNull()
        .onStart { Timber.i("Started current message flow for $tag") }
        .onCompletion { Timber.i("Completed current message flow for $tag") }

    fun eventFlow(tag: String) = octoPrintFlow()
        .flatMapLatest { it?.getEventWebSocket()?.eventFlow(tag) ?: emptyFlow() }
        .catch { e -> Timber.e(e) }

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
            is OctoEverywhereConnectionNotFoundException, is OctoEverywhereSubscriptionMissingException -> {
                Timber.w("Caught OctoEverywhere exception, removing connection")
                BaseInjector.get().handleOctoEverywhereExceptionUseCase().execute(e)
            }

            else -> Unit
        }
    }

    fun createAdHocOctoPrint(it: OctoPrintInstanceInformationV3) =
        OctoPrint(
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
        ).also { octoPrint ->
            // Setup logger to use timber
            TimberLogger(octoPrint.getLogger())
        }
}
