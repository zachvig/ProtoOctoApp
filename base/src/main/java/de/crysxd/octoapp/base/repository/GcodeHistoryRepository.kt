package de.crysxd.octoapp.base.repository

import de.crysxd.octoapp.base.datasource.DataSource
import de.crysxd.octoapp.base.models.GcodeHistoryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import timber.log.Timber

class GcodeHistoryRepository(
    private val dataSource: DataSource<List<GcodeHistoryItem>>
) {

    private val defaults = listOf("M500", "G28", "G29").map { GcodeHistoryItem(it) }

    fun getHistory() = dataSource.get().let {
        if (it.isNullOrEmpty()) defaults else it
    }

    suspend fun setLabelForGcode(command: String, label: String?) = withContext(Dispatchers.IO) {
        dataSource.store(getHistory().map {
            if (it.command == command) {
                it.copy(label = label)
            } else {
                it
            }
        })
    }

    suspend fun recordGcodeSend(command: String) = withContext(Dispatchers.IO) {
        Timber.d("Record gcode send: $command")
        supervisorScope {
            val current = getHistory()
            val oldItem = getHistory().firstOrNull { it.command == command } ?: GcodeHistoryItem(command)
            val updated = current.toMutableList().also {
                it.remove(oldItem)
                it.add(
                    oldItem.copy(
                        usageCount = oldItem.usageCount + 1,
                        lastUsed = System.currentTimeMillis()
                    )
                )
            }

            dataSource.store(updated)
        }
    }

    suspend fun setFavorite(command: String, favorite: Boolean) = withContext(Dispatchers.IO) {
        supervisorScope {
            dataSource.store(getHistory().map {
                if (it.command == command) {
                    it.copy(isFavorite = favorite)
                } else {
                    it
                }
            })
        }
    }

    suspend fun removeEntry(command: String) = withContext(Dispatchers.IO) {
        supervisorScope {
            dataSource.store(getHistory().filter { it.command != command })
        }
    }
}