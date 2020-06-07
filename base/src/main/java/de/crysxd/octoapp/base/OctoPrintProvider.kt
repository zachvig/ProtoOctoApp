package de.crysxd.octoapp.base

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import de.crysxd.octoapp.base.livedata.PollingLiveData
import de.crysxd.octoapp.base.livedata.WebSocketLiveData
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformation
import de.crysxd.octoapp.base.models.exceptions.InvalidOctoPrintInstanceInformation
import de.crysxd.octoapp.base.models.exceptions.NoPrinterConnectedException
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.octoprint.OctoPrint
import de.crysxd.octoapp.octoprint.exceptions.InvalidApiKeyException
import de.crysxd.octoapp.octoprint.exceptions.OctoPrintException
import de.crysxd.octoapp.octoprint.exceptions.PrinterNotOperationalException
import de.crysxd.octoapp.octoprint.models.printer.PrinterState
import de.crysxd.octoapp.octoprint.models.socket.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.logging.HttpLoggingInterceptor

class OctoPrintProvider(
    private val httpLoggingInterceptor: HttpLoggingInterceptor,
    octoPrintRepository: OctoPrintRepository
) {

    val octoPrint: LiveData<OctoPrint?> =
        Transformations.map(octoPrintRepository.instanceInformation) {
            if (it == null) {
                null
            } else {
                createAdHocOctoPrint(it)
            }
        }

    val printerState: LiveData<PollingLiveData.Result<PrinterState>> =
        Transformations.switchMap(octoPrint) {
            PollingLiveData {
                withContext(Dispatchers.IO) {
                    try {
                        it?.createPrinterApi()?.getPrinterState() ?: throw InvalidApiKeyException()
                    } catch (e: PrinterNotOperationalException) {
                        throw NoPrinterConnectedException()
                    } catch (e: InvalidApiKeyException) {
                        octoPrintRepository.clearOctoprintInstanceInformation()
                        throw InvalidOctoPrintInstanceInformation()
                    } catch (e: OctoPrintException) {
                        throw e
                    }
                }
            }
        }

    val eventLiveData: LiveData<Event> = Transformations.switchMap(octoPrint) {
        if (it == null) {
            MutableLiveData<Event>()
        } else {
            WebSocketLiveData(it.getEventWebSocket())
        }
    }

    fun createAdHocOctoPrint(it: OctoPrintInstanceInformation) =
        OctoPrint(it.hostName, it.port, it.apiKey, listOf(httpLoggingInterceptor))
}