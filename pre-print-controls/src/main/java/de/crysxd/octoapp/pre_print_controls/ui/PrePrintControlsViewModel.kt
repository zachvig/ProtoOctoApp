package de.crysxd.octoapp.pre_print_controls.ui

import android.content.Context
import android.text.InputType
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.sinks.SendGcodeCommandValueSink
import de.crysxd.octoapp.base.ui.BaseViewModel
import de.crysxd.octoapp.base.ui.common.enter_value.EnterValueFragmentArgs
import de.crysxd.octoapp.base.usecase.TurnOffPsuUseCase
import de.crysxd.octoapp.pre_print_controls.R
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class PrePrintControlsViewModel(
    private val octoPrintProvider: OctoPrintProvider,
    private val turnOffPsuUseCase: TurnOffPsuUseCase,
    private val executeGcodeCommandValueSink: SendGcodeCommandValueSink
) : BaseViewModel() {

    private val mutableMessages = MutableLiveData<(Context) -> CharSequence>()
    val messages = Transformations.map(mutableMessages) { it }

    fun turnOffPsu() = GlobalScope.launch(coroutineExceptionHandler) {
        octoPrintProvider.octoPrint.value?.let {
            turnOffPsuUseCase.execute(it)
        }
    }

    fun executeGcodeCommand(context: Context) {
        navContoller.navigate(
            R.id.action_enter_gcode, EnterValueFragmentArgs(
                title = context.getString(R.string.send_gcode),
                hint = context.getString(R.string.gcode_one_command_per_line),
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS,
                maxLines = 10,
                valueSink = executeGcodeCommandValueSink
            ).toBundle()
        )
    }
}