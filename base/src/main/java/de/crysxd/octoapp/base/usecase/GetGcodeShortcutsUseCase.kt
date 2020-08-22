package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.base.models.GcodeHistoryItem
import de.crysxd.octoapp.base.repository.GcodeHistoryRepository
import timber.log.Timber
import javax.inject.Inject

const val MAX_HISTORY_LENGTH = 5

class GetGcodeShortcutsUseCase @Inject constructor(
    private val gcodeHistoryRepository: GcodeHistoryRepository
) : UseCase<Unit, List<GcodeHistoryItem>>() {

    override suspend fun doExecute(param: Unit, timber: Timber.Tree): List<GcodeHistoryItem> {
        val history = gcodeHistoryRepository.getHistory()
        val favs = history.filter { it.isFavorite }.sortedBy { it.command }
        val others = history.filter { !it.isFavorite }.sortedByDescending { it.lastUsed }.take(MAX_HISTORY_LENGTH)
        return listOf(favs, others).flatten()
    }
}