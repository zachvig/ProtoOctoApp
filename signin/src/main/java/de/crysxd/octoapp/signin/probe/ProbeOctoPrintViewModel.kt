package de.crysxd.octoapp.signin.probe

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.base.ui.base.BaseViewModel
import de.crysxd.octoapp.base.usecase.TestFullNetworkStackUseCase
import de.crysxd.octoapp.base.utils.AnimationTestUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

class ProbeOctoPrintViewModel(
    private val useCase: TestFullNetworkStackUseCase,
    private val octoPrintRepository: OctoPrintRepository,
) : BaseViewModel() {

    companion object {
        private const val MIN_PROBE_DURATION = 2000L
        private const val MIN_TEST_PROBE_DURATION = 1000L
    }

    var lastWebUrl: HttpUrl? = null
        private set
    var probeIsActive: Boolean = false
    private val mutableUiState = MutableLiveData<UiState>(UiState.Loading)
    val uiState = mutableUiState.map { it }

    fun probe(webUrl: String) = viewModelScope.launch(coroutineExceptionHandler) {
        // Don't allow consecutive probes
        if (probeIsActive) return@launch

        try {
            probeIsActive = true
            val start = System.currentTimeMillis()
            mutableUiState.postValue(UiState.Loading)
            val apiKey = octoPrintRepository.findInstances(webUrl.toHttpUrlOrNull()).firstOrNull()?.first?.apiKey ?: ""
            val target = TestFullNetworkStackUseCase.Target.OctoPrint(webUrl = webUrl, apiKey = apiKey)
            val finding = useCase.execute(target)
            val delay = if (AnimationTestUtils.animationsDisabled) MIN_TEST_PROBE_DURATION else MIN_PROBE_DURATION
            (delay - (System.currentTimeMillis() - start)).takeIf { it > 0 }?.let { delay(it) }
            lastWebUrl = finding.webUrl
            mutableUiState.postValue(UiState.FindingsReady(finding))
        } finally {
            probeIsActive = false
        }
    }

    sealed class UiState {
        object Loading : UiState()
        data class FindingsReady(val finding: TestFullNetworkStackUseCase.Finding, var handled: Boolean = false) : UiState()
    }
}