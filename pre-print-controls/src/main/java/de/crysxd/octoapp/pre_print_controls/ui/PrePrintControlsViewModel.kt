package de.crysxd.octoapp.pre_print_controls.ui

import android.content.Context
import android.text.InputType
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.ui.BaseViewModel
import de.crysxd.octoapp.base.ui.common.enter_value.EnterValueFragmentArgs
import de.crysxd.octoapp.base.ui.navigation.NavigationResultMediator
import de.crysxd.octoapp.base.usecase.ExecuteGcodeCommandUseCase
import de.crysxd.octoapp.base.usecase.TurnOffPsuUseCase
import de.crysxd.octoapp.octoprint.models.printer.GcodeCommand
import de.crysxd.octoapp.pre_print_controls.R
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class PrePrintControlsViewModel(
    private val octoPrintProvider: OctoPrintProvider,
    private val turnOffPsuUseCase: TurnOffPsuUseCase,
    private val executeGcodeCommandUseCase: ExecuteGcodeCommandUseCase
) : BaseViewModel() {

    fun turnOffPsu() = GlobalScope.launch(coroutineExceptionHandler) {
        octoPrintProvider.octoPrint.value?.let {
            turnOffPsuUseCase.execute(it)
        }
    }

    private fun onGcodeEntered(gcode: String) = GlobalScope.launch(coroutineExceptionHandler) {
        octoPrintProvider.octoPrint.value?.let {
            val gcodeCommand = GcodeCommand.Batch(gcode.split("\n").toTypedArray())
            executeGcodeCommandUseCase.execute(Pair(it, gcodeCommand))
            postMessage { con ->
                con.getString(
                    if (gcodeCommand.commands.size == 1) {
                        R.string.sent_x
                    } else {
                        R.string.sent_x_and_y_others
                    }, gcodeCommand.commands.first(), gcodeCommand.commands.size - 1
                )
            }
        }
    }

    fun executeGcode(context: Context) {
        navContoller.navigate(
            R.id.action_enter_gcode, EnterValueFragmentArgs(
                title = context.getString(R.string.send_gcode),
                hint = context.getString(R.string.gcode_one_command_per_line),
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS,
                maxLines = 10,
                resultId = NavigationResultMediator.registerResultCallback(this::onGcodeEntered)
            ).toBundle()
        )
    }

    fun startPrint() {
        navContoller.navigate(R.id.action_start_print)
    }
}