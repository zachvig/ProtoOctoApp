package de.crysxd.octoapp.base

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import de.crysxd.octoapp.base.logging.TimberHandler
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformationV2
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.octoprint.OctoPrint
import de.crysxd.octoapp.octoprint.SubjectAlternativeNameCompatVerifier
import de.crysxd.octoapp.octoprint.models.socket.Event
import de.crysxd.octoapp.octoprint.models.socket.Message
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.logging.Level

@Suppress("EXPERIMENTAL_API_USAGE")
class OctoPrintProvider(
    private val timberHandler: TimberHandler,
    private val invalidApiKeyInterceptor: InvalidApiKeyInterceptor,
    private val octoPrintRepository: OctoPrintRepository,
    private val analytics: FirebaseAnalytics,
    private val sslKeyStoreHandler: SslKeyStoreHandler
) {

    private val octoPrintMutex = Mutex()
    private var octoPrintCache: Pair<OctoPrintInstanceInformationV2, OctoPrint>? = null
    private val currentMessageChannel = ConflatedBroadcastChannel<Message.CurrentMessage>()

    @Deprecated("Use octoPrintFlow")
    val octoPrint: LiveData<OctoPrint?> = octoPrintFlow().asLiveData()

    @Deprecated("Use eventFlow")
    val eventLiveData: LiveData<Event> = eventFlow("OctoPrintProvider@legacy").asLiveData()

    init {
        // Passively collect data for the analytics profile
        // The passive event flow does not actively open a connection but piggy-backs other Flows
        GlobalScope.launch {
            passiveEventFlow()
                .onEach {
                    updateAnalyticsProfileWithEvents(it)
                    ((it as? Event.MessageReceived)?.message as? Message.CurrentMessage)?.let(currentMessageChannel::offer)
                }
                .retry { delay(1000); true }
                .collect()
        }
    }

    fun octoPrintFlow() = octoPrintRepository.instanceInformationFlow().map {
        octoPrintMutex.withLock {
            when {
                it == null -> null
                octoPrintCache?.first != it -> {
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

    fun passiveEventFlow() = octoPrintFlow()
        .flatMapLatest { it?.getEventWebSocket()?.passiveEventFlow() ?: emptyFlow() }
        .catch { e -> Timber.e(e) }

    fun passiveCurrentMessageFlow() = currentMessageChannel.asFlow()

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

    fun createAdHocOctoPrint(it: OctoPrintInstanceInformationV2) =
        OctoPrint(
            rawWebUrl = it.webUrl,
            apiKey = it.apiKey,
            interceptors = listOf(invalidApiKeyInterceptor),
            keyStore = sslKeyStoreHandler.loadKeyStore(),
            hostnameVerifier = SubjectAlternativeNameCompatVerifier().takeIf { _ -> sslKeyStoreHandler.isWeakVerificationForHost(it.webUrl) },
            connectTimeoutMs = Firebase.remoteConfig.getLong("web_socket_connect_timeout_ms"),
            readWriteTimeout = Firebase.remoteConfig.getLong("web_socket_ping_pong_timeout_ms")
        ).also { octoPrint ->
            val logger = octoPrint.getLogger()
            logger.handlers.forEach { logger.removeHandler(it) }
            logger.addHandler(timberHandler)
            logger.level = Level.ALL
            logger.useParentHandlers = false
        }
}