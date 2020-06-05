package de.crysxd.octoapp.pre_print_controls.ui

import android.content.Context
import android.text.InputType
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Transformations
import androidx.navigation.fragment.findNavController
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.ui.BaseViewModel
import de.crysxd.octoapp.base.ui.common.EnterValueFragment
import de.crysxd.octoapp.base.ui.common.EnterValueFragmentArgs
import de.crysxd.octoapp.base.ui.navigation.NavigationResultMediator
import de.crysxd.octoapp.base.usecase.ExecuteGcodeCommandUseCase
import de.crysxd.octoapp.base.usecase.TurnOffPsuUseCase
import de.crysxd.octoapp.octoprint.models.printer.GcodeCommand
import de.crysxd.octoapp.pre_print_controls.R
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

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

    fun executeGcodeCommand(context: Context) {
        navContoller.navigate(
            R.id.action_enter_gcode, EnterValueFragmentArgs(
                title = context.getString(R.string.send_gcode),
                hint = context.getString(R.string.gcode_one_command_per_line),
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS,
                maxLines = 10,
                valueSink = SendGcodeCommandValueSink()
            ).toBundle()
        )
    }

    @Parcelize
    class SendGcodeCommandValueSink : EnterValueFragment.ValueSink {

        override fun useValue(value: String) {
            GlobalScope.launch {
                Injector.get().octoPrintProvider().octoPrint.value?.let {
                    val gcodeCommand = GcodeCommand.Batch(value.split("\n").toTypedArray())
                    Injector.get().executeGcodeCommandUseCase().execute(Pair(it, gcodeCommand))
                }
            }
        }
    }
}