package de.crysxd.octoapp.base.repository

import de.crysxd.octoapp.base.datasource.DataSource
import de.crysxd.octoapp.base.models.GcodeHistoryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.withContext

@Suppress("EXPERIMENTAL_API_USAGE")
class GcodeHistoryRepository(
    private val dataSource: DataSource<List<GcodeHistoryItem>>
) {

    private val defaults = listOf("M500", "G28", "G29").map { GcodeHistoryItem(it) }
    private val historyChannel = ConflatedBroadcastChannel(defaults)
    val history get() = historyChannel.asFlow()

    init {
        dataSource.store(dataSource.get() ?: defaults)
        pushUpdateToChannel()

    }

    private fun pushUpdateToChannel() {
        historyChannel.offer(dataSource.get()?.sortedByDescending { it.lastUsed } ?: defaults)
    }

    private suspend fun updateHistoryForCommand(command: String, update: (GcodeHistoryItem) -> GcodeHistoryItem?) = withContext(Dispatchers.IO) {
        val history = (dataSource.get()?.toMutableList() ?: defaults.toMutableList())
        val old = history.firstOrNull { it.command == command } ?: GcodeHistoryItem(command)
        history.remove(old)
        update(old)?.let(history::add)
        dataSource.store(history)
        pushUpdateToChannel()
    }

    suspend fun setLabelForGcode(command: String, label: String?) = updateHistoryForCommand(command) {
        it.copy(label = label)
    }

    suspend fun recordGcodeSend(command: String) = updateHistoryForCommand(command) {
        it.copy(usageCount = it.usageCount + 1, lastUsed = System.currentTimeMillis())
    }

    suspend fun setFavorite(command: String, favorite: Boolean) = updateHistoryForCommand(command) {
        it.copy(isFavorite = favorite)
    }

    suspend fun removeEntry(command: String) = updateHistoryForCommand(command) {
        null
    }
}