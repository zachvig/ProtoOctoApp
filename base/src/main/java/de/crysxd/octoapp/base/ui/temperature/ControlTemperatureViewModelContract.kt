package de.crysxd.octoapp.base.ui.temperature

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.PollingLiveData
import de.crysxd.octoapp.base.ui.BaseViewModel
import de.crysxd.octoapp.base.usecase.SetBedTargetTemperatureUseCase
import de.crysxd.octoapp.base.usecase.UseCase
import de.crysxd.octoapp.octoprint.OctoPrint
import de.crysxd.octoapp.octoprint.models.printer.PrinterState
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

abstract class ControlTemperatureViewModelContract(
    private val octoPrintProvider: OctoPrintProvider,
    private val useCase: UseCase<Pair<OctoPrint, Int>, Unit>
) : BaseViewModel() {

    protected abstract val manualOverwriteLiveData: MutableLiveData<PrinterState.ComponentTemperature?>
    private val temperatureMediator: MediatorLiveData<PrinterState.ComponentTemperature?> by lazy { initMediator() }
    val temperature: LiveData<PrinterState.ComponentTemperature?> by lazy { Transformations.map(temperatureMediator) { it } }
    private val octoPrintTempLiveData = Transformations.map(octoPrintProvider.printerState) { s ->
        (s as? PollingLiveData.Result.Success)?.result?.temperature?.let(::extractComponentTemperature)
    }

    private fun initMediator() = MediatorLiveData<PrinterState.ComponentTemperature?>().apply {
        addSource(octoPrintTempLiveData) { temperatureMediator.postValue(it) }
        addSource(manualOverwriteLiveData) { temperatureMediator.postValue(it) }
    }

    fun setTemperature(temp: Int) {
        manualOverwriteLiveData.postValue(temperatureMediator.value?.copy(target = temp.toFloat()))
        GlobalScope.launch(coroutineExceptionHandler) {
            octoPrintProvider.octoPrint.value?.let {
                useCase.execute(Pair(it, temp))
            }
        }
    }

    protected abstract fun extractComponentTemperature(pst: PrinterState.PrinterTemperature): PrinterState.ComponentTemperature

    @StringRes
    abstract fun getComponentName(): Int
}