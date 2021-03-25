package de.crysxd.octoapp.base.repository

import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.octoprint.models.socket.HistoricTemperatureData
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.flow.*
import timber.log.Timber


@Suppress("EXPERIMENTAL_API_USAGE")
class TemperatureDataRepository(
    private val octoPrintProvider: OctoPrintProvider
) {

    companion object {
        const val CHANNEL_BUFFER_SIZE = 100
        const val MAX_ENTRIES = 1000
    }

    private val data = mutableListOf<HistoricTemperatureData>()
    private val channel = BroadcastChannel<List<HistoricTemperatureData>>(CHANNEL_BUFFER_SIZE)

    init {
        GlobalScope.launch(Dispatchers.Default) {
            Timber.i("Collecting temperatures")
            octoPrintProvider.passiveCurrentMessageFlow("temperature-repository")
                .onEach {
                    if (it.isHistoryMessage) {
                        Timber.i("Received history message, clearing data")
                        data.clear()
                    }

                    data.addAll(it.temps)
                    channel.offer(it.temps)

                    if (data.size > MAX_COMMUNICATION_ENTRIES) {
                        data.removeAll(data.take(data.size - MAX_ENTRIES))
                    }
                }
                .retry { Timber.e(it); delay(100); true }
                .collect()
        }.invokeOnCompletion {
            Timber.i("Collecting completed")
            it?.let(Timber::wtf)
        }
    }

    fun flow(includeOld: Boolean = false) = flow {
        if (includeOld) {
            // Copy data to prevent concurrent modification
            emit(data.toList())
        }
        emitAll(channel.asFlow())
    }
}