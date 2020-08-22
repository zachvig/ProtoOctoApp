package de.crysxd.octoapp.base.ui.common.terminal

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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


const val KEY_SELECTED_TERMINAL_FILTERS = "selected_terminal_filters"
const val KEY_STYLED_TERMINAL = "styled_terminal"

class TerminalViewModel(
    private val getGcodeShortcutsUseCase: GetGcodeShortcutsUseCase,
    private val executeGcodeCommandUseCase: ExecuteGcodeCommandUseCase,
    private val serialCommunicationLogsRepository: SerialCommunicationLogsRepository,
    private val getTerminalFiltersUseCase: GetTerminalFiltersUseCase,
    octoPrintProvider: OctoPrintProvider,
    private val sharedPreferences: SharedPreferences,
    private val gson: Gson
) : BaseViewModel() {

    private val terminalFiltersMediator = MediatorLiveData<List<Settings.TerminalFilter>>()
    val terminalFilters = terminalFiltersMediator.map { it }

    var selectedTerminalFilters: List<Settings.TerminalFilter> = loadSelectedFilters()
        set(value) {
            field = value
            saveSelectedFilters()
        }

    private var clearedFrom: Date = Date(0)
    private val mutableGcodes = MutableLiveData<List<GcodeHistoryItem>>()
    val gcodes = mutableGcodes.map { it }

    init {
        // Load gcode history and selected filters
        updateGcodes()
        loadSelectedFilters()

        // Init terminal filters
        terminalFiltersMediator.addSource(liveData {
            val filters = getTerminalFiltersUseCase.execute(Unit)
            upgradeSelectedFilters(filters)
            emit(filters)
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

    private fun saveSelectedFilters() {
        sharedPreferences.edit {
            putString(KEY_SELECTED_TERMINAL_FILTERS, gson.toJson(selectedTerminalFilters))
        }
    }

    private fun upgradeSelectedFilters(filters: List<Settings.TerminalFilter>) {
        selectedTerminalFilters = loadSelectedFilters().map { filter ->
            filters.firstOrNull { it.name == filter.name } ?: filter
        }
    }

    private fun loadSelectedFilters() = sharedPreferences.getString(KEY_SELECTED_TERMINAL_FILTERS, null)?.let {
        gson.fromJson<List<Settings.TerminalFilter>>(it, object : TypeToken<List<Settings.TerminalFilter?>>() {}.type)
    } ?: emptyList()

    fun isStyledTerminalUsed() = sharedPreferences.getBoolean(KEY_STYLED_TERMINAL, true)

    fun setStyledTerminalUsed(used: Boolean) = sharedPreferences.edit { putBoolean(KEY_STYLED_TERMINAL, used) }

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
