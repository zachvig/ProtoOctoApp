package de.crysxd.octoapp.base.ui.widget.gcode

import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.material.snackbar.Snackbar
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.ui.BaseViewModel
import de.crysxd.octoapp.base.ui.OctoActivity
import de.crysxd.octoapp.base.usecase.ExecuteGcodeCommandUseCase
import de.crysxd.octoapp.base.usecase.ExecuteGcodeCommandUseCase.Response.RecordedResponse
import de.crysxd.octoapp.base.usecase.GetGcodeShortcutsUseCase
import de.crysxd.octoapp.octoprint.models.printer.GcodeCommand
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

@Suppress("EXPERIMENTAL_API_USAGE")
class SendGcodeWidgetViewModel(
    private val getGcodeShortcutsUseCase: GetGcodeShortcutsUseCase,
    private val sendGcodeCommandUseCase: ExecuteGcodeCommandUseCase
) : BaseViewModel() {

    private val tutorialHiddenPreferenceKey = "gcode_shortcut_tutorial_hidden"
    val gcodes = flow {
        emit(getGcodeShortcutsUseCase.execute(Unit))
    }.flatMapLatest {
        it
    }.distinctUntilChanged().asLiveData()


    fun sendGcodeCommand(command: String) = viewModelScope.launch(coroutineExceptionHandler) {
        val gcodeCommand = GcodeCommand.Batch(command.split("\n").toTypedArray())

        postMessage(
            OctoActivity.Message.SnackbarMessage(
                duration = Snackbar.LENGTH_INDEFINITE,
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