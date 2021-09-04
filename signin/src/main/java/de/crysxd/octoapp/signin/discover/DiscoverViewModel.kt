package de.crysxd.octoapp.signin.discover

import android.net.Uri
import androidx.lifecycle.asLiveData
import de.crysxd.octoapp.base.billing.BillingManager
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.logging.SensitiveDataMask
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformationV3
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.base.ui.base.BaseViewModel
import de.crysxd.octoapp.base.usecase.DiscoverOctoPrintUseCase
import de.crysxd.octoapp.base.utils.AnimationTestUtils
import de.crysxd.octoapp.signin.R
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import timber.log.Timber

@Suppress("EXPERIMENTAL_API_USAGE")
class DiscoverViewModel(
    private val discoverOctoPrintUseCase: DiscoverOctoPrintUseCase,
    private val octoPrintRepository: OctoPrintRepository,
    private val sensitiveDataMask: SensitiveDataMask,
) : BaseViewModel() {

    companion object {
        const val INITIAL_DELAY_TIME = 3000L
        const val TEST_DELAY = 2000L
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
    }.onEach {
        val delay = getLoadingDelay()
        if (it.previouslyConnectedOptions.isEmpty() && delay > 0) {
            delay(delay)
        }
    }

    val uiState = stateChannel.asFlow().combine(options) { state, options ->
        when {
            state is UiState.Manual -> state
            options.previouslyConnectedOptions.isNotEmpty() || options.discoveredOptions.isNotEmpty() -> options
            else -> UiState.ManualIdle
        }
    }.debounce(300).asLiveData()

    fun getLoadingDelay(): Long {
        val timeSinceCreated = System.currentTimeMillis() - viewModelCreationTime
        val delay = if (AnimationTestUtils.animationsDisabled) TEST_DELAY else INITIAL_DELAY_TIME
        return delay - timeSinceCreated
    }

    fun deleteInstance(webUrl: String) {
        octoPrintRepository.remove(webUrl)
        updatePreviouslyConnectedTrigger.trySend(Unit)
    }

    fun activatePreviouslyConnected(octoPrint: OctoPrintInstanceInformationV3) {
        octoPrintRepository.get(octoPrint.id)?.let {
            octoPrintRepository.setActive(it)
        }
    }

    fun testWebUrl(webUrl: String) {
        try {
            val upgradedUrl = upgradeUrl(webUrl)
            sensitiveDataMask.registerWebUrl(upgradedUrl.toHttpUrlOrNull())

            if (webUrl.isBlank()) {
                throw IllegalArgumentException("URL is empty")
            }

            stateChannel.trySend(UiState.ManualSuccess(upgradedUrl))
        } catch (e: Exception) {
            manualFailureCounter++
            stateChannel.trySend(
                UiState.ManualError(
                    message = Injector.get().localizedContext().getString(R.string.sign_in___discovery___error_invalid_url),
                    exception = e,
                    errorCount = manualFailureCounter
                )
            )
        }
    }

    fun moveToManualState() {
        stateChannel.trySend(UiState.ManualIdle)
    }

    fun moveToOptionsState() {
        stateChannel.trySend(UiState.Loading)
    }

    fun upgradeUrl(webUrl: String): String {
        val upgradedWebUrl = if (!webUrl.startsWith("http://") && !webUrl.startsWith("https://")) {
            "http://${webUrl}"
        } else {
            webUrl
        }
        val loginMarker = "/login/?redirect="
        val withoutLogin = if (upgradedWebUrl.contains(loginMarker)) {
            Timber.i("Removed $loginMarker from URL")
            upgradedWebUrl.take(upgradedWebUrl.indexOf(loginMarker))
        } else {
            upgradedWebUrl
        }
        return Uri.parse(withoutLogin).buildUpon()
            .clearQuery()
            .fragment("")
            .build()
            .toString()
    }

    sealed class UiState {
        object Loading : UiState()
        data class Options(
            val previouslyConnectedOptions: List<OctoPrintInstanceInformationV3>,
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