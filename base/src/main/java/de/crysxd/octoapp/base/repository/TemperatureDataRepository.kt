package de.crysxd.octoapp.base.repository

import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.octoprint.models.printer.PrinterState
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
    private val channel = BroadcastChannel<List<TemperatureSnapshot>>(CHANNEL_BUFFER_SIZE)

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
                    if (data.size > MAX_COMMUNICATION_ENTRIES) {
                        data.removeAll(data.take(data.size - MAX_ENTRIES))
                    }
                    if (data.isNotEmpty()) {
                        val snapshot = listOf("tool0", "tool1", "tool2", "tool3", "bed", "chamber").mapNotNull { component ->
                            val lastData = data.last().components[component] ?: return@mapNotNull null

                            val history = data.map {
                                TemperatureHistoryPoint(
                                    time = it.time,
                                    temperature = it.components[component]?.target ?: 0f
                                )
                            }

                            TemperatureSnapshot(
                                history = history,
                                component = component,
                                current = lastData,
                            )
                        }

                        channel.offer(snapshot)
                    }
                }
                .retry { Timber.e(it); delay(100); true }
                .collect()
        }.invokeOnCompletion {
            Timber.i("Collecting completed")
            it?.let(Timber::wtf)
        }
    }

    fun flow() = channel.asFlow()

    data class TemperatureSnapshot(
        val component: String,
        val current: PrinterState.ComponentTemperature,
        val history: List<TemperatureHistoryPoint>
    )

    data class TemperatureHistoryPoint(
        val temperature: Float,
        val time: Long,
    )
}