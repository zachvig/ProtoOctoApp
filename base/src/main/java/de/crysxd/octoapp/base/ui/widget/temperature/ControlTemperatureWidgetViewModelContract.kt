package de.crysxd.octoapp.base.ui.widget.temperature

import android.content.Context
import android.text.InputType
import androidx.annotation.StringRes
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.livedata.OctoTransformations.filter
import de.crysxd.octoapp.base.livedata.OctoTransformations.filterEventsForMessageType
import de.crysxd.octoapp.base.livedata.OctoTransformations.map
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.ui.BaseViewModel
import de.crysxd.octoapp.base.ui.common.enter_value.EnterValueFragmentArgs
import de.crysxd.octoapp.base.ui.navigation.NavigationResultMediator
import de.crysxd.octoapp.base.usecase.UseCase
import de.crysxd.octoapp.octoprint.OctoPrint
import de.crysxd.octoapp.octoprint.models.printer.PrinterState
import de.crysxd.octoapp.octoprint.models.socket.HistoricTemperatureData
import de.crysxd.octoapp.octoprint.models.socket.Message
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

abstract class ControlTemperatureWidgetViewModelContract(
    private val octoPrintProvider: OctoPrintProvider,
    private val useCase: UseCase<Pair<OctoPrint, Int>, Unit>
) : BaseViewModel() {

    val temperature = octoPrintProvider.eventLiveData
        .filterEventsForMessageType(Message.CurrentMessage::class.java)
        .filter { it.temps.isNotEmpty() }
        .map { extractComponentTemperature(it.temps.first()) }

    private fun setTemperature(temp: Int) = GlobalScope.launch(coroutineExceptionHandler) {
        octoPrintProvider.octoPrint.value?.let {
            useCase.execute(Pair(it, temp))
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

    protected abstract fun extractComponentTemperature(temp: HistoricTemperatureData): PrinterState.ComponentTemperature

    @StringRes
    abstract fun getComponentName(): Int

}