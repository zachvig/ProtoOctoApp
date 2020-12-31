package de.crysxd.octoapp.base.billing

import androidx.lifecycle.asLiveData
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import de.crysxd.octoapp.base.ui.BaseViewModel
import de.crysxd.octoapp.base.utils.LongDuration
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.toList
import org.json.JSONObject
import timber.log.Timber

@Suppress("EXPERIMENTAL_API_USAGE")
class PurchaseViewModel : BaseViewModel() {

    private val viewStateChannel = ConflatedBroadcastChannel<ViewState>(ViewState.InitState)
    val viewState = BillingManager.billingFlow().combine(viewStateChannel.asFlow()) { billingData, viewState ->
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
        viewStateChannel.offer(ViewState.SkuSelectionState())
    }

    fun moveToInitState() {
        viewStateChannel.offer(ViewState.InitState)
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