package de.crysxd.octoapp.base.repository

import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.models.SerialCommunication
import de.crysxd.octoapp.octoprint.models.socket.Event
import de.crysxd.octoapp.octoprint.models.socket.Message
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*

const val MAX_COMMUNICATION_ENTRIES = 250

@Suppress("EXPERIMENTAL_API_USAGE")
class SerialCommunicationLogsRepository(
    private val octoPrintProvider: OctoPrintProvider
) {

    private val logs = mutableListOf<SerialCommunication>()
    private val channel = ConflatedBroadcastChannel<List<SerialCommunication>>(emptyList())

    init {
        GlobalScope.launch {
            Timber.i("Collecting serial communication")
            octoPrintProvider.passiveEventFlow()
                .mapNotNull { it as Event.MessageReceived }
                .mapNotNull { it.message as Message.CurrentMessage }
                .onEach {
                    if (it.isHistoryMessage) {
                        logs.clear()
                    }

                    val date = getDate(it.serverTime)
                    val newLogs = it.logs.map {
                        SerialCommunication(it, date)
                    }

                    logs.addAll(newLogs)
                    channel.send(newLogs)

                    if (logs.size > MAX_COMMUNICATION_ENTRIES) {
                        logs.removeAll(logs.take(MAX_COMMUNICATION_ENTRIES - logs.size))
                    }
                }
                .retry { Timber.e(it); delay(100); true }
                .collect()
        }.invokeOnCompletion { it?.let(Timber::wtf) }
    }

    fun flow() = flow<List<SerialCommunication>> {
        emit(all())
        emitAll(channel.asFlow())
    }

    fun all() = logs.toList()

    private fun getDate(serverTime: Double) = Date((serverTime * 1000 * 1000 / 1000).toLong())
}