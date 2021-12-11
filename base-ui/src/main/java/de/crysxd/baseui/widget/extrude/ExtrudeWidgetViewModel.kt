package de.crysxd.baseui.widget.extrude

import android.content.Context
import android.text.InputType
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import de.crysxd.baseui.BaseViewModel
import de.crysxd.baseui.OctoActivity
import de.crysxd.baseui.R
import de.crysxd.baseui.common.enter_value.EnterValueFragmentArgs
import de.crysxd.baseui.utils.NavigationResultMediator
import de.crysxd.octoapp.base.network.OctoPrintProvider
import de.crysxd.octoapp.base.usecase.BaseChangeTemperaturesUseCase
import de.crysxd.octoapp.base.usecase.ExtrudeFilamentUseCase
import de.crysxd.octoapp.base.usecase.SetTargetTemperaturesUseCase
import de.crysxd.octoapp.base.utils.AppScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class ExtrudeWidgetViewModel(
    private val extrudeFilamentUseCase: ExtrudeFilamentUseCase,
    private val setTargetTemperatureUseCase: SetTargetTemperaturesUseCase,
    octoPrintProvider: OctoPrintProvider
) : BaseViewModel() {

    var isCurrentlyVisible = true
        private set
    val isVisible = octoPrintProvider.passiveCurrentMessageFlow("extrude-widget").map {
        // Widget is visible if we are not printing (printing, pausing, paused, cancelling) or we are paused
        isCurrentlyVisible = it.state?.flags?.let { flags ->
            !flags.isPrinting() || flags.paused
        } ?: true
        isCurrentlyVisible
    }.distinctUntilChanged().asLiveData()

    fun extrude5mm() = extrude(5)

    fun extrude50mm() = extrude(50)

    fun extrude100mm() = extrude(100)

    fun extrude120mm() = extrude(120)

    fun extrudeOther(context: Context) = viewModelScope.launch(coroutineExceptionHandler) {
        val result = NavigationResultMediator.registerResultCallback<String?>()

        navContoller.navigate(
            R.id.action_enter_value,
            EnterValueFragmentArgs(
                title = context.getString(R.string.extrude_retract),
                hint = context.getString(R.string.distance_in_mm_negative_for_retract),
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED,
                resultId = result.first
            ).toBundle()
        )

        withContext(Dispatchers.Default) {
            result.second.asFlow().first()
        }?.let {
            extrude(it.toInt())
        }
    }

    private fun extrude(mm: Int) = AppScope.launch(coroutineExceptionHandler) {
        try {
            postMessage(OctoActivity.Message.SnackbarMessage { it.getString(R.string.extruding_x_mm, mm) })
            extrudeFilamentUseCase.execute(ExtrudeFilamentUseCase.Param(mm))
        } catch (e: ExtrudeFilamentUseCase.ColdExtrusionException) {
            postMessage(
                OctoActivity.Message.DialogMessage(
                    text = { it.getString(R.string.error_cold_extrusion, e.minTemp, e.currentTemp) },
                    neutralButton = { it.getString(R.string.heat_hotend) },
                    neutralAction = {
                        AppScope.launch(coroutineExceptionHandler) {
                            Timber.i("Heating to ${e.minTemp} before extrusion")
                            setTargetTemperatureUseCase.execute(
                                BaseChangeTemperaturesUseCase.Params(
                                    BaseChangeTemperaturesUseCase.Temperature(
                                        component = "tool0",
                                        temperature = e.minTemp + 5
                                    )
                                )
                            )
                            postMessage(OctoActivity.Message.SnackbarMessage { it.getString(R.string.heating_hotend, e.minTemp) })
                        }
                    }
                )
            )
        }
    }
}