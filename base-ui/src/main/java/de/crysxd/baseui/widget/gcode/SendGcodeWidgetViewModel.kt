package de.crysxd.baseui.widget.gcode

import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import de.crysxd.baseui.BaseViewModel
import de.crysxd.baseui.OctoActivity
import de.crysxd.baseui.R
import de.crysxd.octoapp.base.OctoPreferences
import de.crysxd.octoapp.base.network.OctoPrintProvider
import de.crysxd.octoapp.base.usecase.ExecuteGcodeCommandUseCase
import de.crysxd.octoapp.base.usecase.ExecuteGcodeCommandUseCase.Response.RecordedResponse
import de.crysxd.octoapp.base.usecase.GetGcodeShortcutsUseCase
import de.crysxd.octoapp.octoprint.models.printer.GcodeCommand
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Suppress("EXPERIMENTAL_API_USAGE")
class SendGcodeWidgetViewModel(
    private val octoPreferences: OctoPreferences,
    private val octoPrintProvider: OctoPrintProvider,
    private val getGcodeShortcutsUseCase: GetGcodeShortcutsUseCase,
    private val sendGcodeCommandUseCase: ExecuteGcodeCommandUseCase
) : BaseViewModel() {

    val gcodes = flow {
        emit(getGcodeShortcutsUseCase.execute(Unit))
    }.flatMapLatest {
        it
    }.distinctUntilChanged().asLiveData()
    var isCurrentlyVisible = true
        private set
    val isVisible = octoPrintProvider.passiveCurrentMessageFlow("gcode-widget").map {
        // Widget is visible if we are not printing (printing, pausing, paused, cancelling) or we are paused or we are allowed to always show
        val printing = it.state?.flags?.isPrinting() ?: true
        val paused = it.state?.flags?.paused ?: false
        val alwaysShown = octoPreferences.allowTerminalDuringPrint
        isCurrentlyVisible = !printing || paused || alwaysShown
        isCurrentlyVisible
    }.distinctUntilChanged().asLiveData()

    suspend fun needsConfirmation() = octoPrintProvider.passiveCurrentMessageFlow("gcode-widget-check").first().state?.flags?.isPrinting() ?: true

    fun sendGcodeCommand(command: String) = viewModelScope.launch(coroutineExceptionHandler) {
        val gcodeCommand = GcodeCommand.Batch(command.split("\n").toTypedArray())

        postMessage(
            OctoActivity.Message.SnackbarMessage(
                text = { con ->
                    con.getString(
                        if (gcodeCommand.commands.size == 1) {
                            R.string.sent_x
                        } else {
                            R.string.sent_x_and_y_others
                        }, gcodeCommand.commands.first(), gcodeCommand.commands.size - 1
                    )
                },
                debounce = true
            )
        )

        val responses = sendGcodeCommandUseCase.execute(
            ExecuteGcodeCommandUseCase.Param(
                command = gcodeCommand,
                fromUser = true,
                recordResponse = true
            )
        )

        postMessage(
            OctoActivity.Message.SnackbarMessage(
                type = OctoActivity.Message.SnackbarMessage.Type.Positive,
                text = { con ->
                    con.getString(
                        if (gcodeCommand.commands.size == 1) {
                            R.string.received_response_for_x
                        } else {
                            R.string.received_response_for_x_and_y_others
                        }, gcodeCommand.commands.first(), gcodeCommand.commands.size - 1
                    )
                },
                action = {
                    postMessage(
                        OctoActivity.Message.DialogMessage(
                            text = {
                                responses.joinToString("\n\n") {
                                    listOf(
                                        listOf((it as? RecordedResponse)?.sendLine ?: ""),
                                        (it as? RecordedResponse)?.responseLines ?: emptyList()
                                    ).flatten().joinToString("\n")
                                }
                            }
                        )
                    )
                },
                actionText = { it.getString(R.string.show_logs) }
            )
        )
    }
}