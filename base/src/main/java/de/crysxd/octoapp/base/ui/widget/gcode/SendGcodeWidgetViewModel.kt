package de.crysxd.octoapp.base.ui.widget.gcode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.ui.BaseViewModel
import de.crysxd.octoapp.base.usecase.ExecuteGcodeCommandUseCase
import de.crysxd.octoapp.octoprint.models.printer.GcodeCommand
import kotlinx.coroutines.launch

class SendGcodeWidgetViewModel(
    private val octoPrintProvider: OctoPrintProvider,
    private val sendGcodeCommandUseCase: ExecuteGcodeCommandUseCase
) : BaseViewModel() {

    fun sendGcodeCommand(command: String) = viewModelScope.launch(coroutineExceptionHandler) {
        octoPrintProvider.octoPrint.value?.let {
            sendGcodeCommandUseCase.execute(Pair(it, GcodeCommand.Single(command)))
        }
    }
}