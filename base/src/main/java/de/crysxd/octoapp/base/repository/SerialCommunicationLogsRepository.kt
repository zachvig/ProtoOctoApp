package de.crysxd.octoapp.base.repository

import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.octoprint.models.socket.Event
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.launch
import timber.log.Timber

class SerialCommunicationLogsRepository(
    private val octoPrintProvider: OctoPrintProvider
) {

    private var collectJob: Job? = null

    fun startWithScope(coroutineScope: CoroutineScope) {
        collectJob?.cancel()
        collectJob = coroutineScope.launch {
            octoPrintProvider.eventFlow("SerialCommunicationLogsRepository")
                .mapNotNull { it as? Event.MessageReceived }
                .onEach {
                    Timber.i("Message: ${it::class.java}")
                }
                .retry { Timber.e(it); delay(100); true }
                .collect()
        }
        collectJob?.invokeOnCompletion { it?.let(Timber::e) }
    }
}