package de.crysxd.octoapp.base.ui.widget.gcode

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.google.android.material.snackbar.Snackbar
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.models.GcodeHistoryItem
import de.crysxd.octoapp.base.repository.GcodeHistoryRepository
import de.crysxd.octoapp.base.ui.BaseViewModel
import de.crysxd.octoapp.base.usecase.ExecuteGcodeCommandUseCase
import de.crysxd.octoapp.base.usecase.ExecuteGcodeCommandUseCase.Response.RecordedResponse
import de.crysxd.octoapp.base.usecase.GetGcodeShortcutsUseCase
import de.crysxd.octoapp.octoprint.models.printer.GcodeCommand
import kotlinx.coroutines.launch


class SendGcodeWidgetViewModel(
    private val gcodeHistoryRepository: GcodeHistoryRepository,
    private val getGcodeShortcutsUseCase: GetGcodeShortcutsUseCase,
    private val sendGcodeCommandUseCase: ExecuteGcodeCommandUseCase
) : BaseViewModel() {

    private val mutableGcodes = MutableLiveData<List<GcodeHistoryItem>>()
    val gcodes = mutableGcodes.map { it }

    init {
        updateGcodes()
    }

    private fun updateGcodes() = viewModelScope.launch(coroutineExceptionHandler) {
        mutableGcodes.postValue(getGcodeShortcutsUseCase.execute(Unit))
    }

    fun setFavorite(gcode: GcodeHistoryItem, favorite: Boolean) = viewModelScope.launch(coroutineExceptionHandler) {
        gcodeHistoryRepository.setFavorite(gcode.command, favorite).join()
        updateGcodes()
    }

    fun sendGcodeCommand(command: String, updateViewAfterDone: Boolean = false) = viewModelScope.launch(coroutineExceptionHandler) {
        val gcodeCommand = GcodeCommand.Batch(command.split("\n").toTypedArray())

        postMessage(
            Message.SnackbarMessage(
                duration = Snackbar.LENGTH_INDEFINITE,
                text = { con ->
                    con.getString(
                        if (gcodeCommand.commands.size == 1) {
                            R.string.sent_x
                        } else {
                            R.string.sent_x_and_y_others
                        }, gcodeCommand.commands.first(), gcodeCommand.commands.size - 1
                    )
                }
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
            Message.SnackbarMessage(
                type = Message.SnackbarMessage.Type.Positive,
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
                    postMessage(Message.DialogMessage {
                        responses.map {
                            listOf(
                                listOf((it as? RecordedResponse)?.sendLine ?: ""),
                                (it as? RecordedResponse)?.responseLines ?: emptyList()
                            ).flatten().joinToString("\n")
                        }.joinToString("\n\n")
                    })
                },
                actionText = { it.getString(R.string.show_logs) }
            )
        )

        if (updateViewAfterDone) {
            updateGcodes()
        }
    }
}