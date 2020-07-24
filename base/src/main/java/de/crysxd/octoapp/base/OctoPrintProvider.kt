package de.crysxd.octoapp.base

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import de.crysxd.octoapp.base.livedata.OctoTransformations.map
import de.crysxd.octoapp.base.livedata.WebSocketLiveData
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformation
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.octoprint.OctoPrint
import de.crysxd.octoapp.octoprint.models.socket.Event
import de.crysxd.octoapp.octoprint.models.socket.Message
import okhttp3.logging.HttpLoggingInterceptor
import timber.log.Timber

class OctoPrintProvider(
    private val httpLoggingInterceptor: HttpLoggingInterceptor,
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
                    param("firmware_name", data.firmwareName)
                    param("machine_type", data.machineType)
                    param("extruder_count", data.extruderCount.toLong())
                }
                analytics.setUserProperty("printer_firmware_name", data.firmwareName)
                analytics.setUserProperty("printer_machine_type", data.machineType)
                analytics.setUserProperty("printer_extruder_count", data.extruderCount.toString())
            }

            it is Event.Error -> Timber.tag("Websocket").w(it.exception)
        }

        it
    }

    fun createAdHocOctoPrint(it: OctoPrintInstanceInformation) =
        OctoPrint(it.hostName, it.port, it.apiKey, listOf(invalidApiKeyInterceptor, httpLoggingInterceptor))
}