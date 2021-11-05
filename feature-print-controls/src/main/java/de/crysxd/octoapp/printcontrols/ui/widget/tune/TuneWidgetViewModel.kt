package de.crysxd.octoapp.printcontrols.ui.widget.tune

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import de.crysxd.baseui.BaseViewModel
import de.crysxd.octoapp.base.data.models.SerialCommunication
import de.crysxd.octoapp.base.data.repository.SerialCommunicationLogsRepository
import de.crysxd.octoapp.base.usecase.ExecuteGcodeCommandUseCase
import de.crysxd.octoapp.base.utils.PollingLiveData
import de.crysxd.octoapp.octoprint.models.printer.GcodeCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.regex.Matcher
import java.util.regex.Pattern

const val SETTINGS_POLLING_INTERVAL_MS = 30000L
const val SETTINGS_POLLING_INITIAL_DELAY = 500L

class TuneWidgetViewModel(
    serialCommunicationLogsRepository: SerialCommunicationLogsRepository,
    private val executeGcodeCommandUseCase: ExecuteGcodeCommandUseCase
) : BaseViewModel() {

    private val uiStateMediator = MediatorLiveData<UiState>()
    val uiState = uiStateMediator.map { it }

    // Matches either
    // "N1907 M221 S78*96" -> Flow rate command send during print
    // "Recv: echo:E0 Flow: 100%" -> Response to M221 command
    private val flowRatePattern = Pattern.compile("(M221 S|Recv.*Flow: )(\\d+)")

    // Matches all of:
    // "N1907 M220 S78*96" -> Feed rate command send during print
    // "Recv: FR:78%" -> Response to M220 command
    private val feedRatePattern = Pattern.compile("(M220 S|Recv.*FR:)(\\d+)")

    // Matches all of
    // "M106 S255" -> M106 command to set fan
    // "M107" -> M107 command to turn fan off
    private val fanSpeedPattern = Pattern.compile("(M106|M107)( S(\\d+))?")

    // Matches all of
    // "Recv: echo:Probe OffsetZ: 0.01" -> Response to offset change
    // "Recv: echo:Probe Offset Z0.01" -> Response to offset change
    private val zOffsetPattern = Pattern.compile("Probe Offset\\s*:?\\s*Z:?\\s*(-?\\d+\\.\\d+)")

    private val pollValuesLiveData = PollingLiveData(interval = SETTINGS_POLLING_INTERVAL_MS) {
        // Response is picked up from serial communications stream, so we don't need to do anything here
        delay(SETTINGS_POLLING_INITIAL_DELAY)
        pollSettingsNow()
    }

    init {
        uiStateMediator.addSource(
            serialCommunicationLogsRepository.flow(includeOld = true)
                .onEach {
                extractValues(it)
            }
            .retry { Timber.e(it); delay(100); true }
            .flowOn(Dispatchers.Default)
            .asLiveData()
        ) {

        }
        uiStateMediator.addSource(pollValuesLiveData) {}
        uiStateMediator.postValue(UiState())
    }

    private suspend fun extractValues(comm: SerialCommunication) {
        extractValue(flowRatePattern.matcher(comm.content), 2) { state, value -> state.copy(flowRate = value.toInt()) }
        extractValue(feedRatePattern.matcher(comm.content), 2) { state, value -> state.copy(feedRate = value.toInt()) }
        extractValue(fanSpeedPattern.matcher(comm.content), 3) { state, value -> state.copy(fanSpeed = ((value.toInt() / 255f) * 100f).toInt()) }
        extractValue(zOffsetPattern.matcher(comm.content), 1) { state, value -> state.copy(zOffsetMm = value.toFloat()) }
    }

    fun pollSettingsNow() = viewModelScope.launch {
        try {
            doPollSettings()
        } catch (e: Exception) {
            Timber.e(e)
            // We do not report this error to the user
        }
    }

    private suspend fun doPollSettings() {
        executeGcodeCommandUseCase.execute(
            ExecuteGcodeCommandUseCase.Param(
                command = GcodeCommand.Batch(arrayOf("M220", "M221")),
                fromUser = false
            )
        )
    }

    private suspend fun extractValue(matcher: Matcher, groupIndex: Int, upgrade: (UiState, String) -> UiState) {
        if (matcher.find() && matcher.groupCount() < groupIndex + 1) {
            val value = matcher.group(groupIndex) ?: return Timber.w("Regex matched but no value: $matcher")
            val oldState = uiStateMediator.value ?: UiState()
            val newState = upgrade(oldState, value)
            if (oldState != newState) {
                Timber.i("Upgraded state after serial communication value was extracted: $oldState -> $newState")
                withContext(Dispatchers.Main) {
                    uiStateMediator.value = newState
                }
            }
        }
    }

    data class UiState(
        val flowRate: Int? = null,
        val feedRate: Int? = null,
        val fanSpeed: Int? = null,
        val zOffsetMm: Float? = null,
    )
}