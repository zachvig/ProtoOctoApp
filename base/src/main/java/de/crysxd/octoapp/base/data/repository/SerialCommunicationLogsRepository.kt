package de.crysxd.octoapp.base.data.repository

import de.crysxd.octoapp.base.data.models.SerialCommunication
import de.crysxd.octoapp.base.network.OctoPrintProvider
import de.crysxd.octoapp.base.utils.AppScope
import de.crysxd.octoapp.octoprint.models.socket.Event
import de.crysxd.octoapp.octoprint.models.socket.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Date


class SerialCommunicationLogsRepository(
    private val octoPrintProvider: OctoPrintProvider
) {

    companion object {
        const val MAX_COMMUNICATION_ENTRIES = 1000
    }

    private val logs = mutableListOf<SerialCommunication>()
    private val flow = MutableSharedFlow<SerialCommunication?>(0, 10, BufferOverflow.SUSPEND)

    init {
        AppScope.launch(Dispatchers.Default) {
            Timber.i("Collecting serial communication")
            octoPrintProvider.passiveEventFlow()
                .mapNotNull { it as? Event.MessageReceived }
                .mapNotNull { it.message as? Message.CurrentMessage }
                .onEach {
                    if (it.isHistoryMessage) {
                        Timber.i("History message received, clearing cache")
                        logs.clear()
                    }

                    val serverDate = getDate(it.serverTime)
                    val newLogs = it.logs.map { log ->
                        SerialCommunication(
                            content = log,
                            date = Date(),
                            serverDate = serverDate,
                            source = SerialCommunication.Source.OctoPrint
                        )
                    }

                    logs.addAll(newLogs)
                    newLogs.forEach { sc -> flow.emit(sc) }

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

    suspend fun addInternalLog(log: String, fromUser: Boolean) {
        flow.emit(
            SerialCommunication(
                content = log,
                serverDate = null,
                date = Date(),
                source = if (fromUser) SerialCommunication.Source.User else SerialCommunication.Source.OctoAppInternal
            )
        )
    }

    fun flow(includeOld: Boolean = false) = flow {
        if (includeOld) {
            all().forEach { emit(it) }
        }
        emitAll(flow)
    }.filterNotNull()

    fun all() = logs.toList()

    private fun getDate(serverTime: Double) = Date((serverTime * 1000 * 1000 / 1000).toLong())
}