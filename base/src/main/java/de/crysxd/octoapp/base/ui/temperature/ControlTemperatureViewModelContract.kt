package de.crysxd.octoapp.base.ui.temperature

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.PollingLiveData
import de.crysxd.octoapp.base.ui.BaseViewModel
import de.crysxd.octoapp.octoprint.models.printer.PrinterState

abstract class ControlTemperatureViewModelContract(octoPrintProvider: OctoPrintProvider) :
    BaseViewModel() {

    private val manualOverwriteLiveData = MutableLiveData<PrinterState.ComponentTemperature?>()
    private val octoPrintTempLiveData = Transformations.map(octoPrintProvider.printerState) { s ->
        (s as? PollingLiveData.Result.Success)?.result?.temperature?.let(::extractComponentTemperature)
    }
    private val temperatureMediator = MediatorLiveData<PrinterState.ComponentTemperature?>()
    val temperature: LiveData<PrinterState.ComponentTemperature?> =
        Transformations.map(temperatureMediator) { it }

    init {
        temperatureMediator.addSource(octoPrintTempLiveData) { temperatureMediator.postValue(it) }
        temperatureMediator.addSource(manualOverwriteLiveData) { temperatureMediator.postValue(it) }
    }

    protected abstract fun extractComponentTemperature(pst: PrinterState.PrinterTemperature): PrinterState.ComponentTemperature

    fun setTemperature(temp: Int) {
        applyTemperature(temp)
        manualOverwriteLiveData.postValue(temperatureMediator.value?.copy(target = temp.toFloat()))
    }

    abstract protected fun applyTemperature(temp: Int)

    @StringRes
    abstract fun getComponentName(): Int

}