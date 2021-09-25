package de.crysxd.octoapp.base.data.repository

import de.crysxd.octoapp.base.data.models.GcodeHistoryItem
import de.crysxd.octoapp.base.data.source.DataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class GcodeHistoryRepository(
    private val dataSource: DataSource<List<GcodeHistoryItem>>
) {

    private val defaults = listOf("M500", "G28", "G29").map { GcodeHistoryItem(it) }
    private val historyFlow = MutableStateFlow(defaults)
    val history get() = historyFlow.asStateFlow()

    init {
        dataSource.store(dataSource.get() ?: defaults)
        pushUpdateToChannel()
    }

    private fun pushUpdateToChannel() {
        historyFlow.value = dataSource.get()?.sortedByDescending { it.lastUsed } ?: defaults
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