package de.crysxd.octoapp.signin.discover

import androidx.lifecycle.asLiveData
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformationV2
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.base.ui.base.BaseViewModel
import de.crysxd.octoapp.base.usecase.DiscoverOctoPrintUseCase
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import timber.log.Timber

class DiscoverViewModel(
    private val discoverOctoPrintUseCase: DiscoverOctoPrintUseCase,
    private val octoPrintRepository: OctoPrintRepository,
) : BaseViewModel() {

    companion object {
        const val INITIAL_LOADING_DELAY_MS = 2000L
    }

    private val reloadTrigger = ConflatedBroadcastChannel(Unit)
    private val updateConnectedTrigger = ConflatedBroadcastChannel(Unit)
    val uiState = reloadTrigger.asFlow().flatMapLatest {
        discoverOctoPrintUseCase.execute(Unit)
    }.combine(updateConnectedTrigger.asFlow()) { it, _ ->
        Timber.i("Discovered ${it.discovered.size} instances")
        UiState(
            error = it.error,
            discoveredOctoPrint = it.discovered,
            connectedOctoPrint = octoPrintRepository.getAll()
        )
    }.onStart {
        // Nothing connected yet? Keep search state for a bit more
        if (octoPrintRepository.getAll().isEmpty()) {
            delay(INITIAL_LOADING_DELAY_MS)
        }
    }.asLiveData()

    fun reload() {
        reloadTrigger.offer(Unit)
    }

    fun deleteInstance(webUrl: String) {
        octoPrintRepository.remove(webUrl)
        updateConnectedTrigger.offer(Unit)
    }

    data class UiState(
        val connectedOctoPrint: List<OctoPrintInstanceInformationV2>,
        val discoveredOctoPrint: List<DiscoverOctoPrintUseCase.DiscoveredOctoPrint>,
        val error: Exception? = null
    )
}