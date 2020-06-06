package de.crysxd.octoapp.base.ui.temperature

import android.content.Context
import android.text.InputType
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.PollingLiveData
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.ui.BaseViewModel
import de.crysxd.octoapp.base.ui.common.enter_value.EnterValueFragmentArgs
import de.crysxd.octoapp.base.ui.navigation.NavigationResultMediator
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

    private fun setTemperature(temp: Int) {
        manualOverwriteLiveData.postValue(temperatureMediator.value?.copy(target = temp.toFloat()))
        GlobalScope.launch(coroutineExceptionHandler) {
            octoPrintProvider.octoPrint.value?.let {
                useCase.execute(Pair(it, temp))
            }
        }
    }

    fun changeTemperature(context: Context) {
        navContoller.navigate(
            R.id.action_enter_temperature,
            EnterValueFragmentArgs(
                title = context.getString(R.string.x_temperature, context.getString(getComponentName())),
                hint = context.getString(R.string.target_temperature),
                resultId = NavigationResultMediator.registerResultCallback(this::onTemperatureEntered),
                value = temperature.value?.target?.toInt()?.toString(),
                inputType = InputType.TYPE_CLASS_NUMBER,
                selectAll = true
            ).toBundle()
        )
    }

    private fun onTemperatureEntered(temp: String) {
        setTemperature(temp.toInt())
    }

    protected abstract fun extractComponentTemperature(pst: PrinterState.PrinterTemperature): PrinterState.ComponentTemperature

    @StringRes
    abstract fun getComponentName(): Int

}