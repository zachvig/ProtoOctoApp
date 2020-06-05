package de.crysxd.octoapp.pre_print_controls.ui

import android.content.Context
import android.text.InputType
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Transformations
import androidx.navigation.fragment.findNavController
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.ui.BaseViewModel
import de.crysxd.octoapp.base.ui.common.EnterValueFragmentArgs
import de.crysxd.octoapp.base.ui.navigation.NavigationResultMediator
import de.crysxd.octoapp.base.usecase.ExecuteGcodeCommandUseCase
import de.crysxd.octoapp.base.usecase.TurnOffPsuUseCase
import de.crysxd.octoapp.octoprint.models.printer.GcodeCommand
import de.crysxd.octoapp.pre_print_controls.R
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

class PrePrintControlsViewModel(
    private val octoPrintProvider: OctoPrintProvider,
    private val turnOffPsuUseCase: TurnOffPsuUseCase,
    private val executeGcodeCommandUseCase: ExecuteGcodeCommandUseCase
) : BaseViewModel() {

    private var lastGcodeSource: LiveData<String>? = null
    private val gcodeCommandsMediator = MediatorLiveData<String>()

    init {
        viewModelLiveDatas.addSource(gcodeCommandsMediator) {
            executeGcode(it)
        }
    }

    fun turnOffPsu() = GlobalScope.launch(coroutineExceptionHandler) {
        octoPrintProvider.octoPrint.value?.let {
            turnOffPsuUseCase.execute(it)
        }
    }

    private fun waitForGcodeCommand(): Int {
        lastGcodeSource?.let(gcodeCommandsMediator::removeSource)
        val registration = NavigationResultMediator.registerResultCallback<String>()
        lastGcodeSource = registration.second
        gcodeCommandsMediator.addSource(lastGcodeSource!!) { gcodeCommandsMediator.postValue(it) }
        return registration.first
    }

    private fun executeGcode(gcode: String) = GlobalScope.launch(coroutineExceptionHandler) {
        octoPrintProvider.octoPrint.value?.let {
            val gcodeCommand = GcodeCommand.Batch(gcode.toUpperCase(Locale.ENGLISH).split("\n").toTypedArray())
            executeGcodeCommandUseCase.execute(Pair(it, gcodeCommand))
        }
    }

    fun executeGcodeCommand(context: Context) {
        val resultId = waitForGcodeCommand()

        navContoller.navigate(
            R.id.action_enter_gcode, EnterValueFragmentArgs(
            title = context.getString(R.string.send_gcode),
            hint = context.getString(R.string.gcode_one_command_per_line),
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS,
            maxLines = 10,
            resultId = resultId
        ).toBundle())


    }
}