package de.crysxd.octoapp.base.ui.widget.temperature

import android.content.Context
import android.text.InputType
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.ext.rateLimit
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.base.repository.TemperatureDataRepository
import de.crysxd.octoapp.base.ui.base.BaseViewModel
import de.crysxd.octoapp.base.ui.common.enter_value.EnterValueFragmentArgs
import de.crysxd.octoapp.base.ui.navigation.NavigationResultMediator
import de.crysxd.octoapp.base.usecase.SetTargetTemperaturesUseCase
import de.crysxd.octoapp.octoprint.models.profiles.PrinterProfiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class ControlTemperatureWidgetViewModel(
    temperatureDataRepository: TemperatureDataRepository,
    private val octoPrintRepository: OctoPrintRepository,
    private val setTargetTemperaturesUseCase: SetTargetTemperaturesUseCase,
) : BaseViewModel() {

    private val printerProfile = octoPrintRepository.instanceInformationFlow().map {
        it?.activeProfile ?: PrinterProfiles.Profile()
    }
    val temperature = temperatureDataRepository.flow().combine(printerProfile) { temps, profile ->
        temps.filter {
            val isChamber = it.component == "chamber" && profile.heatedChamber
            val isBed = it.component == "bed" && profile.heatedBed
            val isOther = it.component != "bed" && it.component != "chamber"
            isOther || isChamber || isBed
        }
    }.retry {
        Timber.e(it)
        delay(1000)
        true
    }.asLiveData()

    fun getInitialComponentCount() = octoPrintRepository.getActiveInstanceSnapshot()?.activeProfile?.let {
        var counter = 1
        if (it.heatedBed) counter++
        if (it.heatedChamber) counter++
        counter
    } ?: 2

    private suspend fun setTemperature(temp: Int, component: String) {
        setTargetTemperaturesUseCase.execute(
            SetTargetTemperaturesUseCase.Params(
                SetTargetTemperaturesUseCase.Temperature(temperature = temp, component = component)
            )
        )
    }

    fun changeTemperature(context: Context, component: String) = viewModelScope.launch(coroutineExceptionHandler) {
        val result = NavigationResultMediator.registerResultCallback<String?>()
        val current = temperature.value?.firstOrNull { it.component == component }?.current?.actual?.toInt()?.toString()

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

    fun getMaxTemp(component: String) = when (component) {
        "tool0" -> 250
        "tool1" -> 250
        "tool3" -> 250
        "tool4" -> 250
        "bed" -> 100
        "chamber" -> 100
        else -> 100
    }
}