package de.crysxd.octoapp.signin.discover

import android.net.Uri
import androidx.lifecycle.asLiveData
import de.crysxd.octoapp.base.billing.BillingManager
import de.crysxd.octoapp.base.logging.SensitiveDataMask
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformationV2
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.base.ui.base.BaseViewModel
import de.crysxd.octoapp.base.usecase.DiscoverOctoPrintUseCase
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import timber.log.Timber

@Suppress("EXPERIMENTAL_API_USAGE")
class DiscoverViewModel(
    private val discoverOctoPrintUseCase: DiscoverOctoPrintUseCase,
    private val octoPrintRepository: OctoPrintRepository,
    private val sensitiveDataMask: SensitiveDataMask,
) : BaseViewModel() {

    companion object {
        const val INITIAL_DELAY_TIME = 5000
    }

    private var manualFailureCounter = 0
    private val viewModelCreationTime = System.currentTimeMillis()
    private val updatePreviouslyConnectedTrigger = ConflatedBroadcastChannel(Unit)
    private val stateChannel = ConflatedBroadcastChannel<UiState>(UiState.Loading)
    private val options = flow {
        emit(DiscoverOctoPrintUseCase.Result(emptyList()))
        discoverOctoPrintUseCase.execute(Unit).collect {
            emit(it)
        }
    }.combine(updatePreviouslyConnectedTrigger.asFlow()) { it, _ ->
        Timber.i("Discovered ${it.discovered.size} instances")
        UiState.Options(
            discoveredOptions = it.discovered,
            previouslyConnectedOptions = octoPrintRepository.getAll(),
            supportsQuickSwitch = false
        )
    }.combine(BillingManager.billingFlow()) { uiState, _ ->
        uiState.copy(supportsQuickSwitch = BillingManager.isFeatureEnabled(BillingManager.FEATURE_QUICK_SWITCH))
    }

    val uiState = stateChannel.asFlow().combine(options) { state, options ->
        when {
            state is UiState.Manual -> state
            options.previouslyConnectedOptions.isNotEmpty() || options.discoveredOptions.isNotEmpty() -> options
            else -> UiState.ManualIdle
        }
    }.onEach {
        val timeSinceCreated = System.currentTimeMillis() - viewModelCreationTime
        if (it !is UiState.Loading && timeSinceCreated < INITIAL_DELAY_TIME) {
            delay(INITIAL_DELAY_TIME - timeSinceCreated)
        }
    }.debounce(300).asLiveData()

    fun deleteInstance(webUrl: String) {
        octoPrintRepository.remove(webUrl)
        updatePreviouslyConnectedTrigger.offer(Unit)
    }

    fun activatePreviouslyConnected(octoPrint: OctoPrintInstanceInformationV2) {
        octoPrintRepository.getAll().firstOrNull { it.webUrl == octoPrint.webUrl }?.let {
            octoPrintRepository.setActive(it)
        }
    }

    fun testWebUrl(webUrl: String) {
        val upgradedWebUrl = if (!webUrl.startsWith("http://") && !webUrl.startsWith("https://")) {
            "http://${webUrl}"
        } else {
            webUrl
        }
        sensitiveDataMask.registerWebUrl(upgradedWebUrl, "octoprint")

        try {
            if (webUrl.isBlank()) {
                throw IllegalArgumentException("URL is empty")
            }
            Uri.parse(upgradedWebUrl)
            stateChannel.offer(UiState.ManualSuccess(upgradedWebUrl))
        } catch (e: Exception) {
            manualFailureCounter++
            stateChannel.offer(UiState.ManualError(message = "Please provide a valid URL H", exception = e, errorCount = manualFailureCounter))
        }
    }

    fun moveToManualState() {
        stateChannel.offer(UiState.ManualIdle)
    }

    fun moveToOptionsState() {
        stateChannel.offer(UiState.Loading)
    }

    sealed class UiState {
        object Loading : UiState()
        data class Options(
            val previouslyConnectedOptions: List<OctoPrintInstanceInformationV2>,
            val discoveredOptions: List<DiscoverOctoPrintUseCase.DiscoveredOctoPrint>,
            val supportsQuickSwitch: Boolean,
        ) : UiState()

        abstract class Manual : UiState()

        object ManualIdle : Manual()

        data class ManualError(
            var handled: Boolean = false,
            val errorCount: Int,
            val message: String?,
            val exception: Exception
        ) : Manual()

        data class ManualSuccess(
            val webUrl: String,
            var handled: Boolean = false,
        ) : Manual()
    }

}