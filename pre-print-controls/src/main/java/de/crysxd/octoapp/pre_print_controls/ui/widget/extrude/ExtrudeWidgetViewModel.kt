package de.crysxd.octoapp.pre_print_controls.ui.widget.extrude

import android.content.Context
import android.text.InputType
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.ui.BaseViewModel
import de.crysxd.octoapp.base.ui.common.enter_value.EnterValueFragmentArgs
import de.crysxd.octoapp.base.ui.navigation.NavigationResultMediator
import de.crysxd.octoapp.base.usecase.ExtrudeFilamentUseCase
import de.crysxd.octoapp.pre_print_controls.R
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ExtrudeWidgetViewModel(
    private val octoPrintProvider: OctoPrintProvider,
    private val extrudeFilamentUseCase: ExtrudeFilamentUseCase
) : BaseViewModel() {

    fun extrude5mm() = extrude(5)

    fun extrude50mm() = extrude(50)

    fun extrude100mm() = extrude(100)

    fun extrude120mm() = extrude(120)

    fun extrudeOther(context: Context) {
        navContoller.navigate(
            R.id.action_enter_extrude_distance,
            EnterValueFragmentArgs(
                title = context.getString(R.string.extrude_retract),
                hint = context.getString(R.string.distance_in_mm_negative_for_retract),
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED,
                resultId = NavigationResultMediator.registerResultCallback<String>(this::onDistanceEntered)
            ).toBundle()
        )
    }

    private fun onDistanceEntered(distance: String) {
        extrude(distance.toInt())
    }

    private fun extrude(mm: Int) = GlobalScope.launch(coroutineExceptionHandler) {
        octoPrintProvider.octoPrint.value?.let {
            extrudeFilamentUseCase.execute(Pair(it, mm))
            postMessage { c -> c.getString(R.string.extruding_x_mm, mm) }
        }
    }
}