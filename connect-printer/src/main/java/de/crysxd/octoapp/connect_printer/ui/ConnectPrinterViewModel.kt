package de.crysxd.octoapp.connect_printer.ui

import androidx.lifecycle.Transformations
import androidx.lifecycle.viewModelScope
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.livedata.PollingLiveData
import de.crysxd.octoapp.base.models.exceptions.InvalidOctoPrintInstanceInformation
import de.crysxd.octoapp.base.models.exceptions.NoPrinterConnectedException
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.base.ui.BaseViewModel
import de.crysxd.octoapp.base.usecase.TurnOnPsuUseCase
import de.crysxd.octoapp.octoprint.exceptions.InvalidApiKeyException
import de.crysxd.octoapp.octoprint.exceptions.OctoPrintException
import de.crysxd.octoapp.octoprint.exceptions.PrinterNotOperationalException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ConnectPrinterViewModel(
    private val octoPrintProvider: OctoPrintProvider,
    private val turnOnPsuUseCase: TurnOnPsuUseCase,
    private val octoPrintRepository: OctoPrintRepository
) : BaseViewModel() {

    val printerState = Transformations.switchMap(octoPrintProvider.octoPrint) {
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

    fun turnOnPsu() = viewModelScope.launch(coroutineExceptionHandler) {
        octoPrintProvider.octoPrint.value?.let {
            turnOnPsuUseCase.execute(it)
        }
    }
}