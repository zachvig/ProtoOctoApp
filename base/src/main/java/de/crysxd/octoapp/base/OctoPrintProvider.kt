package de.crysxd.octoapp.base

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import de.crysxd.octoapp.base.logging.TimberHandler
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformationV2
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.octoprint.OctoPrint
import de.crysxd.octoapp.octoprint.models.socket.Event
import de.crysxd.octoapp.octoprint.models.socket.Message
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.util.logging.Level

@Suppress("EXPERIMENTAL_API_USAGE")
class OctoPrintProvider(
    private val timberHandler: TimberHandler,
    private val invalidApiKeyInterceptor: InvalidApiKeyInterceptor,
    octoPrintRepository: OctoPrintRepository,
    private val analytics: FirebaseAnalytics
) {

    private var octoPrintCache: Pair<OctoPrintInstanceInformationV2, OctoPrint>? = null
    val octoPrintFlow = octoPrintRepository.instanceInformationFlow().map {
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

    @Deprecated("Use octoPrintFlow")
    val octoPrint: LiveData<OctoPrint?> = octoPrintFlow.asLiveData()

    val eventFlow = octoPrintFlow
        .flatMapLatest { it?.getEventWebSocket()?.eventFlow() ?: emptyFlow() }
        .map { e -> updateAnalyticsProfileWithEvents(e); e }
        .catch { e -> Timber.e(e) }

    @Deprecated("Use eventFlow")
    val eventLiveData: LiveData<Event> = eventFlow.asLiveData()

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
        }
    }

    fun createAdHocOctoPrint(it: OctoPrintInstanceInformationV2) =
        OctoPrint(it.webUrl, it.apiKey, listOf(invalidApiKeyInterceptor)).also { octoPrint ->
            val logger = octoPrint.getLogger()
            logger.handlers.forEach { logger.removeHandler(it) }
            logger.addHandler(timberHandler)
            logger.level = Level.ALL
            logger.useParentHandlers = false
        }
}