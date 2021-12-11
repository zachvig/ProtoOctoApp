package de.crysxd.baseui.widget.temperature

import android.content.Context
import android.text.InputType
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import de.crysxd.baseui.BaseViewModel
import de.crysxd.baseui.R
import de.crysxd.baseui.common.enter_value.EnterValueFragmentArgs
import de.crysxd.baseui.utils.NavigationResultMediator
import de.crysxd.octoapp.base.data.repository.OctoPrintRepository
import de.crysxd.octoapp.base.data.repository.TemperatureDataRepository
import de.crysxd.octoapp.base.ext.rateLimit
import de.crysxd.octoapp.base.network.OctoPrintProvider
import de.crysxd.octoapp.base.usecase.BaseChangeTemperaturesUseCase
import de.crysxd.octoapp.base.usecase.SetTargetTemperaturesUseCase
import de.crysxd.octoapp.base.usecase.SetTemperatureOffsetUseCase
import de.crysxd.octoapp.base.utils.AnimationTestUtils
import de.crysxd.octoapp.octoprint.models.profiles.PrinterProfiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class ControlTemperatureWidgetViewModel(
    temperatureDataRepository: TemperatureDataRepository,
    private val octoPrintRepository: OctoPrintRepository,
    private val octoPrintProvider: OctoPrintProvider,
    private val setTemperatureOffsetUseCase: SetTemperatureOffsetUseCase,
    private val setTargetTemperaturesUseCase: SetTargetTemperaturesUseCase,
) : BaseViewModel() {

    private val printerProfile = octoPrintRepository.instanceInformationFlow().map {
        it?.activeProfile ?: PrinterProfiles.Profile()
    }
    private var first = true
    val temperature = temperatureDataRepository.flow().onStart {
        first = true
    }.combine(printerProfile) { temps, profile ->
        val input = temps.map { it.copy(history = emptyList()) }
        val output = temps.filter {
            val isChamber = it.component == "chamber" && profile.heatedChamber
            val isBed = it.component == "bed" && profile.heatedBed
            val isTool = it.component.startsWith("tool") && (!profile.extruder.sharedNozzle || it.component == "tool0")
            val isOther = it.component != "chamber" && it.component != "bed" && !it.component.startsWith("tool")
            isOther || isTool || isChamber || isBed
        }

        if (input.size != output.size && first) {
            first = false
            Timber.d("Reducing temperatures: $input -> $output")
        }

        output
    }.combine(octoPrintProvider.passiveCurrentMessageFlow("temperatures")) { temps, current ->
        temps to current
    }.let {
        // Slow down update rate for test
        if (AnimationTestUtils.animationsDisabled) {
            it.rateLimit(10000)
        } else {
            it
        }
    }.retry {
        Timber.e(it)
        delay(1000)
        true
    }.asLiveData()

    fun getInitialComponentCount() = octoPrintRepository.getActiveInstanceSnapshot()?.activeProfile?.let {
        var counter = if (it.extruder.sharedNozzle) 1 else it.extruder.count
        if (it.heatedBed) counter++
        if (it.heatedChamber) counter++
        Timber.i("Using $counter temperature controls for initial setup ($it)")
        counter
    } ?: 2

    private suspend fun setTemperature(temp: Int, component: String) {
        setTargetTemperaturesUseCase.execute(
            BaseChangeTemperaturesUseCase.Params(
                BaseChangeTemperaturesUseCase.Temperature(temperature = temp, component = component)
            )
        )
    }

    private suspend fun setOffset(offset: Int, component: String) {
        setTemperatureOffsetUseCase.execute(
            BaseChangeTemperaturesUseCase.Params(
                BaseChangeTemperaturesUseCase.Temperature(temperature = offset, component = component)
            )
        )
    }

    fun changeTemperature(context: Context, component: String) = viewModelScope.launch(coroutineExceptionHandler) {
        val targetResult = NavigationResultMediator.registerResultCallback<String?>()
        val offsetResult = NavigationResultMediator.registerResultCallback<String?>()
        val (target, offset) = octoPrintProvider.passiveCurrentMessageFlow("change-temperature").first().let {
            val target = it.temps.maxByOrNull { it.time }?.components?.get(component)?.target?.toInt()
            val offset = it.offsets?.get(component)?.toInt()
            target to offset
        }

        navContoller.navigate(
            R.id.action_enter_value,
            EnterValueFragmentArgs(
                title = context.getString(R.string.x_temperature, getComponentName(context, component)),
                action = context.getString(R.string.set_temperature),
                resultId = targetResult.first,
                hint = context.getString(R.string.target_temperature),
                value = target?.toString(),
                inputType = InputType.TYPE_CLASS_NUMBER,
                resultId2 = offsetResult.first,
                hint2 = context.getString(R.string.temperature_widget___change_offset),
                value2 = offset?.toString(),
                inputType2 = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED,
                selectAll = true
            ).toBundle()
        )

        withContext(Dispatchers.Default) {
            targetResult.second.asFlow().first()
        }?.let { temp ->
            val tempInt = temp.toIntOrNull()
            if (tempInt != null && tempInt != target) {
                setTemperature(tempInt, component)
            } else {
                Timber.i("No change, dropping target change to $temp")
            }
        }

        withContext(Dispatchers.Default) {
            offsetResult.second.asFlow().first()
        }?.let { temp ->
            val tempInt = temp.toIntOrNull()
            if (tempInt != null && tempInt != target) {
                setOffset(tempInt, component)
            } else {
                Timber.i("No change, dropping offset change to $temp")
            }
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