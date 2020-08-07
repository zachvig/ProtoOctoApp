package de.crysxd.octoapp.base.ui.widget.gcode

import android.content.Context
import android.text.InputType
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.ui.BaseViewModel
import de.crysxd.octoapp.base.ui.common.enter_value.EnterValueFragmentArgs
import de.crysxd.octoapp.base.ui.navigation.NavigationResultMediator
import de.crysxd.octoapp.base.usecase.ExecuteGcodeCommandUseCase
import de.crysxd.octoapp.octoprint.models.printer.GcodeCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SendGcodeWidgetViewModel(
    private val octoPrintProvider: OctoPrintProvider,
    private val sendGcodeCommandUseCase: ExecuteGcodeCommandUseCase
) : BaseViewModel() {

    fun sendGcodeCommand(command: String) = viewModelScope.launch(coroutineExceptionHandler) {
        val gcodeCommand = GcodeCommand.Batch(command.split("\n").toTypedArray())

        sendGcodeCommandUseCase.execute(ExecuteGcodeCommandUseCase.Param(gcodeCommand, true))
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

    fun sendGcodeCommand(context: Context) = viewModelScope.launch(coroutineExceptionHandler) {
        val result = NavigationResultMediator.registerResultCallback<String?>()

        navContoller.navigate(
            R.id.action_enter_value,
            EnterValueFragmentArgs(
                title = context.getString(R.string.send_gcode),
                hint = context.getString(R.string.gcode_one_command_per_line),
                action = context.getString(R.string.send_gcode),
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS,
                maxLines = 10,
                resultId = result.first
            ).toBundle()
        )

        withContext(Dispatchers.Default) {
            result.second.asFlow().first()
        }?.let {
            sendGcodeCommand(it)
        }
    }
}