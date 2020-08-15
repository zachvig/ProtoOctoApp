package de.crysxd.octoapp.base.repository

import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.models.SerialCommunication
import de.crysxd.octoapp.octoprint.models.socket.Event
import de.crysxd.octoapp.octoprint.models.socket.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*

const val MAX_COMMUNICATION_ENTRIES = 1000
const val CHANNEL_BUFFER_SIZE = 10

@Suppress("EXPERIMENTAL_API_USAGE")
class SerialCommunicationLogsRepository(
    private val octoPrintProvider: OctoPrintProvider
) {

    private val logs = mutableListOf<SerialCommunication>()
    private val channel = BroadcastChannel<SerialCommunication>(CHANNEL_BUFFER_SIZE)

    init {
        GlobalScope.launch(Dispatchers.Default) {
            Timber.i("Collecting serial communication")
            octoPrintProvider.passiveEventFlow()
                .mapNotNull { it as? Event.MessageReceived }
                .mapNotNull { it.message as? Message.CurrentMessage }
                .onEach {
                    if (it.isHistoryMessage) {
                        logs.clear()
                    }

                    val date = getDate(it.serverTime)
                    val newLogs = it.logs.map {
                        SerialCommunication(it, date)
                    }

                    logs.addAll(newLogs)
                    newLogs.forEach {
                        channel.send(it)
                    }

                    if (logs.size > MAX_COMMUNICATION_ENTRIES) {
                        logs.removeAll(logs.take(logs.size - MAX_COMMUNICATION_ENTRIES))
                    }
                }
                .retry { Timber.e(it); delay(100); true }
                .collect()
        }.invokeOnCompletion {
            Timber.i("Collecting completed")
            it?.let(Timber::wtf)
        }
    }

    fun addLog(log: String) {
        channel.offer(SerialCommunication(log, logs.lastOrNull()?.serverTime ?: Date()))
    }

    fun flow(includeOld: Boolean = false) = flow {
        if (includeOld) {
            all().forEach { emit(it) }
        }
        emitAll(channel.asFlow())
    }

    fun all() = logs.toList()

    private fun getDate(serverTime: Double) = Date((serverTime * 1000 * 1000 / 1000).toLong())
}