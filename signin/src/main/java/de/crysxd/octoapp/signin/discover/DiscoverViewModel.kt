package de.crysxd.octoapp.signin.discover

import androidx.lifecycle.asLiveData
import de.crysxd.octoapp.base.billing.BillingManager
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformationV2
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.base.ui.base.BaseViewModel
import de.crysxd.octoapp.base.usecase.DiscoverOctoPrintUseCase
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import timber.log.Timber

class DiscoverViewModel(
    private val discoverOctoPrintUseCase: DiscoverOctoPrintUseCase,
    private val octoPrintRepository: OctoPrintRepository,
) : BaseViewModel() {

    companion object {
        const val INITIAL_LOADING_DELAY_MS = 2000L
    }

    private val updateConnectedTrigger = ConflatedBroadcastChannel(Unit)
    val uiState = flow {
        discoverOctoPrintUseCase.execute(Unit).collect {
            emit(it)
        }
    }.combine(updateConnectedTrigger.asFlow()) { it, _ ->
        Timber.i("Discovered ${it.discovered.size} instances")
        UiState(
            discoveredOctoPrint = it.discovered,
            connectedOctoPrint = octoPrintRepository.getAll(),
            supportsQuickSwitch = false
        )
    }.combine(BillingManager.billingFlow()) { uiState, _ ->
        uiState.copy(supportsQuickSwitch = BillingManager.isFeatureEnabled(BillingManager.FEATURE_QUICK_SWITCH))
    }.onStart {
        // Nothing connected yet? Keep search state for a bit more
        if (octoPrintRepository.getAll().isEmpty()) {
            delay(INITIAL_LOADING_DELAY_MS)
        }
    }.asLiveData()

    fun deleteInstance(webUrl: String) {
        octoPrintRepository.remove(webUrl)
        updateConnectedTrigger.offer(Unit)
    }

    fun activatePreviouslyConnected(octoPrint: OctoPrintInstanceInformationV2) {
        octoPrintRepository.getAll().firstOrNull { it.webUrl == octoPrint.webUrl }?.let {
            octoPrintRepository.setActive(it)
        }
    }

    data class UiState(
        val connectedOctoPrint: List<OctoPrintInstanceInformationV2>,
        val discoveredOctoPrint: List<DiscoverOctoPrintUseCase.DiscoveredOctoPrint>,
        val supportsQuickSwitch: Boolean,
    )
}