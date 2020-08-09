package de.crysxd.octoapp.print_controls.ui.widget.tune

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import de.crysxd.octoapp.base.models.SerialCommunication
import de.crysxd.octoapp.base.repository.SerialCommunicationLogsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retry
import timber.log.Timber
import java.util.regex.Matcher
import java.util.regex.Pattern

class TuneWidgetViewModel(
    serialCommunicationLogsRepository: SerialCommunicationLogsRepository
) : ViewModel() {

    private val uiStateMediator = MediatorLiveData<UiState>()
    val uiState = uiStateMediator.map { it }

    private val flowRatePattern = Pattern.compile("M221 S(\\d+)")
    private val feedRatePattern = Pattern.compile("M220 S(\\d+)")
    private val fanSpeedPattern = Pattern.compile("(M106|M107)( S(\\d+))?")

    init {
        uiStateMediator.addSource(liveData<UiState> {
            serialCommunicationLogsRepository.flow()
                .onEach {
                    it.forEach(this@TuneWidgetViewModel::extractValues)
                }
                .retry { Timber.e(it); delay(100); true }
                .collect()
        }) { uiStateMediator.postValue(it) }

        uiStateMediator.postValue(UiState())
    }

    private fun extractValues(comm: SerialCommunication) {
        extractValue(flowRatePattern.matcher(comm.content), 1) { state, value -> state.copy(flowRate = value) }
        extractValue(feedRatePattern.matcher(comm.content), 1) { state, value -> state.copy(feedRate = value) }
        extractValue(fanSpeedPattern.matcher(comm.content), 3) { state, value -> state.copy(fanSpeed = ((value / 255f) * 100f).toInt()) }
    }

    private fun extractValue(matcher: Matcher, groupIndex: Int, upgrade: (UiState, Int) -> UiState) {
        if (matcher.find()) {
            val value = if (matcher.groupCount() < groupIndex + 1) {
                matcher.group(groupIndex)?.toInt() ?: 0
            } else {
                0
            }
            uiStateMediator.value = upgrade(uiStateMediator.value ?: UiState(), value)
        }
    }

    data class UiState(
        val flowRate: Int = 100,
        val feedRate: Int = 100,
        val fanSpeed: Int = 0
    )
}