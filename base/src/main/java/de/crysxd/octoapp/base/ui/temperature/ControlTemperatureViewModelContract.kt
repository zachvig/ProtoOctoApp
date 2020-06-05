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
        applyTemperature(temp)
        manualOverwriteLiveData.postValue(temperatureMediator.value?.copy(target = temp.toFloat()))
    }

    protected abstract fun extractComponentTemperature(pst: PrinterState.PrinterTemperature): PrinterState.ComponentTemperature

    protected abstract fun applyTemperature(temp: Int)

    @StringRes
    abstract fun getComponentName(): Int
}