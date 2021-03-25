package de.crysxd.octoapp.base.ui.widget.temperature

import android.content.Context
import android.text.InputType
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.repository.TemperatureDataRepository
import de.crysxd.octoapp.base.ui.base.BaseViewModel
import de.crysxd.octoapp.base.ui.common.enter_value.EnterValueFragmentArgs
import de.crysxd.octoapp.base.ui.navigation.NavigationResultMediator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ControlTemperatureWidgetViewModel(
    temperatureDataRepository: TemperatureDataRepository
) : BaseViewModel() {

    val temperature = temperatureDataRepository.flow(true).asLiveData()

    private suspend fun setTemperature(temp: Int, component: String) {
        TODO()
    }

    fun changeTemperature(context: Context, component: String) = viewModelScope.launch(coroutineExceptionHandler) {
        val result = NavigationResultMediator.registerResultCallback<String?>()
        val current = temperature.value?.lastOrNull()?.components?.get(component)?.target?.toInt()?.toString()

        navContoller.navigate(
            R.id.action_enter_value,
            EnterValueFragmentArgs(
                title = context.getString(R.string.x_temperature, getComponentName(context, component)),
                hint = context.getString(R.string.target_temperature),
                action = context.getString(R.string.set_temperature),
                resultId = result.first,
                value = current,
                inputType = InputType.TYPE_CLASS_NUMBER,
                selectAll = true
            ).toBundle()
        )

        withContext(Dispatchers.Default) {
            result.second.asFlow().first()
        }?.let { temp ->
            setTemperature(temp.toInt(), component)
        }
    }

    fun getComponentName(context: Context, component: String) = when (component) {
        "tool0" -> context.getString(R.string.general___hotend)
        "tool1" -> context.getString(R.string.general___hotend_2)
        "tool3" -> context.getString(R.string.general___hotend_3)
        "tool4" -> context.getString(R.string.general___hotend_4)
        "bed" -> context.getString(R.string.general___bed)
        "chamber" -> context.getString(R.string.general___chamber)
        else -> component
    }
}