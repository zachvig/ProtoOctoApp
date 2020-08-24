package de.crysxd.octoapp.print_controls.ui.widget.tune

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import de.crysxd.octoapp.base.livedata.PollingLiveData
import de.crysxd.octoapp.base.models.SerialCommunication
import de.crysxd.octoapp.base.repository.SerialCommunicationLogsRepository
import de.crysxd.octoapp.base.ui.BaseViewModel
import de.crysxd.octoapp.base.usecase.ExecuteGcodeCommandUseCase
import de.crysxd.octoapp.octoprint.models.printer.GcodeCommand
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retry
import timber.log.Timber
import java.util.regex.Matcher
import java.util.regex.Pattern

const val SETTINGS_POLLING_INTERVAL_MS = 30000L
const val SETTINGS_POLLING_INITIAL_DELAY = 500L

class TuneWidgetViewModel(
    serialCommunicationLogsRepository: SerialCommunicationLogsRepository,
    private val executeGcodeCommandUseCase: ExecuteGcodeCommandUseCase
) : BaseViewModel() {

    private var updateJob: Job? = null
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

    val updateLiveData = PollingLiveData(interval = SETTINGS_POLLING_INTERVAL_MS) {
        // Response is picked up from serial communications stream
        delay(SETTINGS_POLLING_INITIAL_DELAY)
        executeGcodeCommandUseCase.execute(
            ExecuteGcodeCommandUseCase.Param(
                command = GcodeCommand.Batch(arrayOf("M220", "M221")),
                fromUser = false
            )
        )
    }

    init {
        uiStateMediator.addSource(serialCommunicationLogsRepository.flow()
            .onEach {
                extractValues(it)
            }
            .retry { Timber.e(it); delay(100); true }
            .asLiveData()
        ) {}

        uiStateMediator.postValue(UiState())
    }

    private fun extractValues(comm: SerialCommunication) {
        extractValue(flowRatePattern.matcher(comm.content), 2) { state, value -> state.copy(flowRate = value) }
        extractValue(feedRatePattern.matcher(comm.content), 2) { state, value -> state.copy(feedRate = value) }
        extractValue(fanSpeedPattern.matcher(comm.content), 3) { state, value -> state.copy(fanSpeed = ((value / 255f) * 100f).toInt()) }
    }

    private fun extractValue(matcher: Matcher, groupIndex: Int, upgrade: (UiState, Int) -> UiState) {
        if (matcher.find()) {
            val value = if (matcher.groupCount() < groupIndex + 1) {
                matcher.group(groupIndex)?.toInt() ?: 0
            } else {
                0
            }

            val oldState = uiStateMediator.value ?: UiState()
            val newState = upgrade(oldState, value)
            uiStateMediator.value = newState
            Timber.i("Upgraded state after serial communication value was extracted: $oldState -> $newState")
        }
    }

    data class UiState(
        val flowRate: Int? = null,
        val feedRate: Int? = null,
        val fanSpeed: Int? = null
    )
}