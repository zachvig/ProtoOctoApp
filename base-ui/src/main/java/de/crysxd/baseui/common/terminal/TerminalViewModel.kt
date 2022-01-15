package de.crysxd.baseui.common.terminal

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import de.crysxd.baseui.BaseViewModel
import de.crysxd.octoapp.base.OctoPreferences
import de.crysxd.octoapp.base.data.models.GcodeHistoryItem
import de.crysxd.octoapp.base.data.models.SerialCommunication
import de.crysxd.octoapp.base.data.repository.OctoPrintRepository
import de.crysxd.octoapp.base.data.repository.SerialCommunicationLogsRepository
import de.crysxd.octoapp.base.network.OctoPrintProvider
import de.crysxd.octoapp.base.usecase.ExecuteGcodeCommandUseCase
import de.crysxd.octoapp.base.usecase.GetGcodeShortcutsUseCase
import de.crysxd.octoapp.base.usecase.GetTerminalFiltersUseCase
import de.crysxd.octoapp.octoprint.models.printer.GcodeCommand
import de.crysxd.octoapp.octoprint.models.settings.Settings
import de.crysxd.octoapp.octoprint.models.socket.Event
import de.crysxd.octoapp.octoprint.models.socket.Message
import de.crysxd.octoapp.octoprint.models.socket.Message.EventMessage.SettingsUpdated
import de.crysxd.octoapp.octoprint.websocket.EventFlowConfiguration
import de.crysxd.octoapp.octoprint.websocket.EventFlowConfiguration.Companion.ALL_LOGS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Date
import java.util.regex.Pattern

@Suppress("EXPERIMENTAL_API_USAGE")
class TerminalViewModel(
    private val getGcodeShortcutsUseCase: GetGcodeShortcutsUseCase,
    private val executeGcodeCommandUseCase: ExecuteGcodeCommandUseCase,
    private val serialCommunicationLogsRepository: SerialCommunicationLogsRepository,
    private val getTerminalFiltersUseCase: GetTerminalFiltersUseCase,
    octoPrintProvider: OctoPrintProvider,
    private val octoPrintRepository: OctoPrintRepository,
    private val octoPreferences: OctoPreferences
) : BaseViewModel() {

    private val terminalFiltersMediator = MediatorLiveData<List<Settings.TerminalFilter>>()
    val terminalFilters = terminalFiltersMediator.map { it }

    var selectedTerminalFilters: List<Settings.TerminalFilter> = loadSelectedFilters()
        set(value) {
            field = value
            saveSelectedFilters()
        }

    private var clearedFrom: Date = Date(0)
    private val printStateFlow = octoPrintProvider.eventFlow(tag = "terminal", config = EventFlowConfiguration(requestTerminalLogs = listOf(ALL_LOGS)))
        .mapNotNull { (it as? Event.MessageReceived)?.message as? Message.CurrentMessage }
        .mapNotNull { it.state?.flags }
        .map { it.pausing || it.cancelling || it.printing }
    val uiState = flow {
        emit(getGcodeShortcutsUseCase.execute(Unit))
    }.flatMapLatest {
        it
    }.combine(printStateFlow) { gcodes, printing ->
        // Combine printing state with setting. If the setting is on, we never get a print state
        UiState(printing && !octoPreferences.allowTerminalDuringPrint, gcodes)
    }.distinctUntilChanged().asLiveData()

    init {
        // Load gcode history and selected filters
        loadSelectedFilters()

        // Init terminal filters
        terminalFiltersMediator.addSource(liveData {
            try {
                val filters = getTerminalFiltersUseCase.execute(Unit)
                upgradeSelectedFilters(filters)
                emit(filters)
            } catch (e: Exception) {
                Timber.e(e)
            }
        }) { terminalFiltersMediator.postValue(it) }

        // Update terminal filters whenever settings are changed
        terminalFiltersMediator.addSource(octoPrintProvider
            .eventFlow(this::class.java.simpleName)
            .mapNotNull { it as? Event.MessageReceived }
            .filter { it.message is SettingsUpdated }
            .map {
                val filters = getTerminalFiltersUseCase.execute(Unit)
                upgradeSelectedFilters(filters)
                filters
            }
            .catch { Timber.e(it) }
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
            serialCommunicationLogsRepository.passiveFlow(false).filter { filter(it) }
        )
    }

    fun clear() {
        serialCommunicationLogsRepository.all().lastOrNull()?.let {
            clearedFrom = it.date
        }
    }

    private fun saveSelectedFilters() = viewModelScope.launch(coroutineExceptionHandler) {
        octoPrintRepository.updateAppSettingsForActive {
            it.copy(selectedTerminalFilters = selectedTerminalFilters)
        }
    }

    private fun upgradeSelectedFilters(filters: List<Settings.TerminalFilter>) {
        selectedTerminalFilters = loadSelectedFilters().map { filter ->
            filters.firstOrNull { it.name == filter.name } ?: filter
        }
    }

    private fun loadSelectedFilters() = octoPrintRepository.getActiveInstanceSnapshot()?.appSettings?.selectedTerminalFilters ?: emptyList()

    fun isStyledTerminalUsed() = octoPrintRepository.getActiveInstanceSnapshot()?.appSettings?.isStyledTerminal ?: true

    fun setStyledTerminalUsed(used: Boolean) = viewModelScope.launch(coroutineExceptionHandler) {
        octoPrintRepository.updateAppSettingsForActive {
            it.copy(isStyledTerminal = used)
        }
    }

    fun executeGcode(gcode: String) = viewModelScope.launch(coroutineExceptionHandler) {
        executeGcodeCommandUseCase.execute(
            ExecuteGcodeCommandUseCase.Param(
                command = GcodeCommand.Batch(gcode.split("\n").filter { it.isNotBlank() }.map { it.trim() }.toTypedArray()),
                fromUser = true,
                recordResponse = false
            )
        )
    }

    data class UiState(
        val printing: Boolean,
        val gcodes: List<GcodeHistoryItem>
    )
}
