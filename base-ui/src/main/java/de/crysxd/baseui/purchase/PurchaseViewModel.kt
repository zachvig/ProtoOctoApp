package de.crysxd.baseui.purchase

import androidx.lifecycle.asLiveData
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import de.crysxd.baseui.BaseViewModel
import de.crysxd.octoapp.base.billing.BillingData
import de.crysxd.octoapp.base.billing.BillingManager
import de.crysxd.octoapp.base.utils.LongDuration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.toList
import org.json.JSONObject
import timber.log.Timber

class PurchaseViewModel : BaseViewModel() {

    private val viewStateFlow = MutableStateFlow<ViewState>(ViewState.InitState)
    val viewState = BillingManager.billingFlow().combine(viewStateFlow) { billingData, viewState ->
        when {
            !billingData.isBillingAvailable -> ViewState.Unsupported
            viewState is ViewState.SkuSelectionState -> viewState.copy(
                billingData = billingData.copy(
                    availableSku = billingData.availableSku.sortedBy { details ->
                        LongDuration.parse(details.subscriptionPeriod)?.inSeconds() ?: Long.MAX_VALUE
                    }
                ),
                names = try {
                    val json = JSONObject(Firebase.remoteConfig.getString("sku_names"))
                    json.keys().asFlow().toList().map { Pair(it, json.getString(it)) }.toMap()
                } catch (e: Exception) {
                    Timber.e(e)
                    emptyMap()
                },
                badges = try {
                    val json = JSONObject(Firebase.remoteConfig.getString("sku_badges"))
                    json.keys().asFlow().toList().map {
                        val badge = when (json.getString(it)) {
                            "best_value" -> Badge.BestValue
                            "popular" -> Badge.Popular
                            else -> Badge.NoBadge
                        }
                        Pair(it, badge)
                    }.toMap()
                } catch (e: Exception) {
                    Timber.e(e)
                    emptyMap()
                }
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
            val names: Map<String, String> = emptyMap(),
            val badges: Map<String, Badge> = emptyMap()
        ) : ViewState()
    }

    sealed class Badge {
        object NoBadge : Badge()
        object Popular : Badge()
        object BestValue : Badge()
    }
}