package de.crysxd.octoapp.pre_print_controls.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Transformations
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.ui.BaseViewModel
import de.crysxd.octoapp.base.ui.navigation.NavigationResultMediator
import de.crysxd.octoapp.base.usecase.ExecuteGcodeCommandUseCase
import de.crysxd.octoapp.base.usecase.TurnOffPsuUseCase
import de.crysxd.octoapp.octoprint.models.printer.GcodeCommand
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class PrePrintControlsViewModel(
    private val octoPrintProvider: OctoPrintProvider,
    private val turnOffPsuUseCase: TurnOffPsuUseCase,
    private val executeGcodeCommandUseCase: ExecuteGcodeCommandUseCase
) : BaseViewModel() {

    private var lastGcodeSource: LiveData<String>? = null
    private val gcodeCommandsMediator = MediatorLiveData<String>()
    val gcodeCommands = Transformations.map(gcodeCommandsMediator) { it }

    fun turnOffPsu() = GlobalScope.launch(coroutineExceptionHandler) {
        octoPrintProvider.octoPrint.value?.let {
            turnOffPsuUseCase.execute(it)
        }
    }

    fun waitForGcodeCommand(): Int {
        lastGcodeSource?.let(gcodeCommandsMediator::removeSource)
        val registration = NavigationResultMediator.registerResultCallback<String>()
        lastGcodeSource = registration.second
        gcodeCommandsMediator.addSource(lastGcodeSource!!) { gcodeCommandsMediator.postValue(it) }
        return registration.first
    }

    fun executeGcode(gcode: String) = GlobalScope.launch(coroutineExceptionHandler) {
        octoPrintProvider.octoPrint.value?.let {
            val gcodeCommand = GcodeCommand.Batch(gcode.split("\n").toTypedArray())
            executeGcodeCommandUseCase.execute(Pair(it, gcodeCommand))
        }
    }
}