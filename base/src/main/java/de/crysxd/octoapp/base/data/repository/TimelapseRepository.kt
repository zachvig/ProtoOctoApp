package de.crysxd.octoapp.base.data.repository

import de.crysxd.octoapp.base.network.OctoPrintProvider
import de.crysxd.octoapp.base.utils.AppScope
import de.crysxd.octoapp.octoprint.models.socket.Event
import de.crysxd.octoapp.octoprint.models.socket.Message.EventMessage.MovieDone
import de.crysxd.octoapp.octoprint.models.socket.Message.EventMessage.MovieFailed
import de.crysxd.octoapp.octoprint.models.socket.Message.EventMessage.MovieRendering
import de.crysxd.octoapp.octoprint.models.timelapse.TimelapseConfig
import de.crysxd.octoapp.octoprint.models.timelapse.TimelapseStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.launch
import timber.log.Timber

class TimelapseRepository(
    private val octoPrintProvider: OctoPrintProvider,
) {

    private val state = MutableStateFlow<TimelapseStatus?>(null)

    init {
        AppScope.launch {
            octoPrintProvider.passiveEventFlow().retry { delay(1000); true }.collect {
                if (it is Event.MessageReceived && (it.message is MovieFailed || it.message is MovieDone || it.message is MovieRendering)) {
                    fetchLatest()
                }
            }
        }
    }

    suspend fun update(block: TimelapseConfig.() -> TimelapseConfig) {
        val current = state.value
        requireNotNull(current) { "Timelapse state not loaded" }
        requireNotNull(current.config) { "Timelapse config not loaded" }
        val new = block(current.config!!)
        Timber.d("Updating timelapse config $current -> $new")
        state.value = octoPrintProvider.octoPrint().createTimelapseApi().updateConfig(new)
    }

    suspend fun fetchLatest(): TimelapseStatus {
        val latest = octoPrintProvider.octoPrint().createTimelapseApi().getStatus()
        state.value = latest
        Timber.d("Got latest timelapse config $latest")
        return latest
    }

    fun peek() = state.value

    fun flow() = state.asStateFlow()
}