package de.crysxd.octoapp.base.ui.widget.temperature

import android.content.Context
import android.text.InputType
import androidx.annotation.StringRes
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.ext.rateLimit
import de.crysxd.octoapp.base.ui.base.BaseViewModel
import de.crysxd.octoapp.base.ui.common.enter_value.EnterValueFragmentArgs
import de.crysxd.octoapp.base.ui.navigation.NavigationResultMediator
import de.crysxd.octoapp.octoprint.models.printer.PrinterState
import de.crysxd.octoapp.octoprint.models.socket.HistoricTemperatureData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class ControlTemperatureWidgetViewModelContract(
    octoPrintProvider: OctoPrintProvider
) : BaseViewModel() {

    val temperature = octoPrintProvider.passiveCurrentMessageFlow("temperature")
        .filter { it.temps.isNotEmpty() }
        .mapNotNull { extractComponentTemperature(it.temps.first()) }
        .rateLimit(1000)
        .asLiveData()

    protected abstract suspend fun setTemperature(temp: Int)

    fun changeTemperature(context: Context) = viewModelScope.launch(coroutineExceptionHandler) {
        val result = NavigationResultMediator.registerResultCallback<String?>()

        navContoller.navigate(
            R.id.action_enter_value,
            EnterValueFragmentArgs(
                title = context.getString(R.string.x_temperature, context.getString(getComponentName())),
                hint = context.getString(R.string.target_temperature),
                action = context.getString(R.string.set_temperature),
                resultId = result.first,
                value = temperature.value?.target?.toInt()?.toString(),
                inputType = InputType.TYPE_CLASS_NUMBER,
                selectAll = true
            ).toBundle()
        )

        withContext(Dispatchers.Default) {
            result.second.asFlow().first()
        }?.let { temp ->
            setTemperature(temp.toInt())
        }
    }

    protected abstract fun extractComponentTemperature(temp: HistoricTemperatureData): PrinterState.ComponentTemperature?

    @StringRes
    abstract fun getComponentName(): Int

}