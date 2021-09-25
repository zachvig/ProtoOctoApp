package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.base.data.models.GcodeHistoryItem
import de.crysxd.octoapp.base.data.repository.GcodeHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject

const val MAX_HISTORY_LENGTH = 5

class GetGcodeShortcutsUseCase @Inject constructor(
    private val gcodeHistoryRepository: GcodeHistoryRepository
) : UseCase<Unit, Flow<List<GcodeHistoryItem>>>() {

    override suspend fun doExecute(param: Unit, timber: Timber.Tree) = gcodeHistoryRepository.history.map { history ->
        val favorites = history.filter { it.isFavorite }.sortedBy { it.command }
        val others = history.filter { !it.isFavorite }.sortedByDescending { it.lastUsed }.take(MAX_HISTORY_LENGTH)
        listOf(favorites, others).flatten()
    }
}