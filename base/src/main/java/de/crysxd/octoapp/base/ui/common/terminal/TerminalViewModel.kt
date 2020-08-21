package de.crysxd.octoapp.base.ui.common.terminal

import androidx.lifecycle.*
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.models.GcodeHistoryItem
import de.crysxd.octoapp.base.models.SerialCommunication
import de.crysxd.octoapp.base.repository.SerialCommunicationLogsRepository
import de.crysxd.octoapp.base.ui.BaseViewModel
import de.crysxd.octoapp.base.usecase.ExecuteGcodeCommandUseCase
import de.crysxd.octoapp.base.usecase.GetGcodeShortcutsUseCase
import de.crysxd.octoapp.base.usecase.GetTerminalFiltersUseCase
import de.crysxd.octoapp.octoprint.models.printer.GcodeCommand
import de.crysxd.octoapp.octoprint.models.settings.Settings
import de.crysxd.octoapp.octoprint.models.socket.Event
import de.crysxd.octoapp.octoprint.models.socket.Message.EventMessage.SettingsUpdated
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import java.util.regex.Pattern

class TerminalViewModel(
    private val getGcodeShortcutsUseCase: GetGcodeShortcutsUseCase,
    private val executeGcodeCommandUseCase: ExecuteGcodeCommandUseCase,
    private val serialCommunicationLogsRepository: SerialCommunicationLogsRepository,
    private val getTerminalFiltersUseCase: GetTerminalFiltersUseCase,
    private val octoPrintProvider: OctoPrintProvider
) : BaseViewModel() {

    private val terminalFiltersMediator = MediatorLiveData<List<Settings.TerminalFilter>>()
    val terminalFilters = terminalFiltersMediator.map { it }

    var selectedTerminalFilters: List<Settings.TerminalFilter> = emptyList()

    private var clearedFrom: Date = Date(0)
    private val mutableGcodes = MutableLiveData<List<GcodeHistoryItem>>()
    val gcodes = mutableGcodes.map { it }

    init {
        // Load gcode history
        updateGcodes()

        // Init terminal filters
        terminalFiltersMediator.addSource(liveData {
            emit(getTerminalFiltersUseCase.execute(Unit))
        }) { terminalFiltersMediator.postValue(it) }

        // Update terminal filters whenever settings are changed
        terminalFiltersMediator.addSource(octoPrintProvider
            .eventFlow(this::class.java.simpleName)
            .mapNotNull { it as? Event.MessageReceived }
            .filter { it.message is SettingsUpdated }
            .map {
                getTerminalFiltersUseCase.execute(Unit)
            }
            .asLiveData()
        ) { terminalFiltersMediator.postValue(it) }
    }

    suspend fun observeSerialCommunication() = withContext(Dispatchers.Default) {
        val filters = selectedTerminalFilters.map { Pattern.compile(it.regex) }
        fun filter(comm: SerialCommunication) = filters.none {
            it.matcher(comm.content).find()
        }

        Pair(
            serialCommunicationLogsRepository.all().filter { it.date > clearedFrom }.filter(::filter),
            serialCommunicationLogsRepository.flow(false).filter { filter(it) }
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
                fromUser = true,
                recordResponse = false
            )
        )
        updateGcodes()
    }
}
