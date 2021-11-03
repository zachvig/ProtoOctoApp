package de.crysxd.baseui.purchase

import androidx.lifecycle.asLiveData
import de.crysxd.baseui.BaseViewModel
import de.crysxd.octoapp.base.billing.BillingData
import de.crysxd.octoapp.base.billing.BillingManager
import de.crysxd.octoapp.base.utils.LongDuration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

class PurchaseViewModel : BaseViewModel() {

    private val viewStateFlow = MutableStateFlow<ViewState>(ViewState.InitState)
    val viewState = BillingManager.billingFlow().combine(viewStateFlow) { billingData, viewState ->
        when {
            !billingData.isBillingAvailable -> ViewState.Unsupported
            viewState is ViewState.SkuSelectionState -> viewState.copy(
                billingData = billingData.copy(
                    allSku = billingData.allSku.sortedBy { details ->
                        LongDuration.parse(details.subscriptionPeriod)?.inSeconds() ?: Long.MAX_VALUE
                    }
                )
            )
            else -> viewState
        }
    }.distinctUntilChanged().asLiveData()

    fun moveToSkuListState() {
        viewStateFlow.value = ViewState.SkuSelectionState()
    }

    fun moveToInitState() {
        viewStateFlow.value = ViewState.InitState
    }

    sealed class ViewState {
        object InitState : ViewState()
        object Unsupported : ViewState()
        data class SkuSelectionState(
            val billingData: BillingData = BillingData(),
        ) : ViewState()
    }
}