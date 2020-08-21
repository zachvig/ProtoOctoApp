package de.crysxd.octoapp.base.ui.common.terminal

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import de.crysxd.octoapp.base.models.GcodeHistoryItem
import de.crysxd.octoapp.base.repository.SerialCommunicationLogsRepository
import de.crysxd.octoapp.base.ui.BaseViewModel
import de.crysxd.octoapp.base.usecase.ExecuteGcodeCommandUseCase
import de.crysxd.octoapp.base.usecase.GetGcodeShortcutsUseCase
import de.crysxd.octoapp.octoprint.models.printer.GcodeCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class TerminalViewModel(
    private val getGcodeShortcutsUseCase: GetGcodeShortcutsUseCase,
    private val executeGcodeCommandUseCase: ExecuteGcodeCommandUseCase,
    private val serialCommunicationLogsRepository: SerialCommunicationLogsRepository
) : BaseViewModel() {

    private var clearedFrom: Date = Date(0)
    private val mutableGcodes = MutableLiveData<List<GcodeHistoryItem>>()
    val gcodes = mutableGcodes.map { it }

    init {
        updateGcodes()
    }

    suspend fun observeSerialCommunication() = withContext(Dispatchers.Default) {
        Pair(
            serialCommunicationLogsRepository.all().filter { it.date > clearedFrom },
            serialCommunicationLogsRepository.flow(false)
        )
    }

    fun clear() {
        serialCommunicationLogsRepository.all().lastOrNull()?.let {
            clearedFrom = it.date
        }
    }

    private fun updateGcodes() = viewModelScope.launch(coroutineExceptionHandler) {
        mutableGcodes.postValue(getGcodeShortcutsUseCase.execute(Unit))
    }

    fun executeGcode(gcode: String) = viewModelScope.launch(coroutineExceptionHandler) {
        executeGcodeCommandUseCase.execute(
            ExecuteGcodeCommandUseCase.Param(
                command = GcodeCommand.Single(gcode),
                fromUser = true
            )
        )
    }
}
