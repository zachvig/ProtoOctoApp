package de.crysxd.octoapp.base

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import de.crysxd.octoapp.base.livedata.OctoTransformations.map
import de.crysxd.octoapp.base.livedata.WebSocketLiveData
import de.crysxd.octoapp.base.logging.TimberHandler
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformationV2
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.octoprint.OctoPrint
import de.crysxd.octoapp.octoprint.models.socket.Event
import de.crysxd.octoapp.octoprint.models.socket.Message
import java.util.logging.Level

class OctoPrintProvider(
    private val timberHandler: TimberHandler,
    private val invalidApiKeyInterceptor: InvalidApiKeyInterceptor,
    octoPrintRepository: OctoPrintRepository,
    private val analytics: FirebaseAnalytics
) {

    val octoPrint: LiveData<OctoPrint?> =
        Transformations.map(octoPrintRepository.instanceInformation) {
            if (it == null) {
                null
            } else {
                createAdHocOctoPrint(it)
            }
        }

    val eventLiveData: LiveData<Event> = Transformations.switchMap(octoPrint) {
        if (it == null) {
            MutableLiveData<Event>()
        } else {
            WebSocketLiveData(it.getEventWebSocket())
        }
    }.map {
        when {
            it is Event.MessageReceived && it.message is Message.EventMessage.FirmwareData -> {
                val data = it.message as Message.EventMessage.FirmwareData
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

        it
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