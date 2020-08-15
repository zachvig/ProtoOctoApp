package de.crysxd.octoapp.pre_print_controls.ui.widget.extrude

import android.content.Context
import android.text.InputType
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.ui.BaseViewModel
import de.crysxd.octoapp.base.ui.common.enter_value.EnterValueFragmentArgs
import de.crysxd.octoapp.base.ui.navigation.NavigationResultMediator
import de.crysxd.octoapp.base.usecase.ExtrudeFilamentUseCase
import de.crysxd.octoapp.pre_print_controls.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExtrudeWidgetViewModel(
    private val octoPrintProvider: OctoPrintProvider,
    private val extrudeFilamentUseCase: ExtrudeFilamentUseCase
) : BaseViewModel() {

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

    private fun extrude(mm: Int) = GlobalScope.launch(coroutineExceptionHandler) {
        extrudeFilamentUseCase.execute(ExtrudeFilamentUseCase.Param(mm))
        postMessage(Message.SnackbarMessage { it.getString(R.string.extruding_x_mm, mm) })
    }
}