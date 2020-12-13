package de.crysxd.octoapp.base.billing

import android.content.Context
import com.android.billingclient.api.*
import com.google.android.gms.tasks.Tasks
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import timber.log.Timber

@Suppress("EXPERIMENTAL_API_USAGE")
object BillingManager {

    private val billingChannel = ConflatedBroadcastChannel(BillingData())
    private val purchasesUpdateListener = PurchasesUpdatedListener { billingResult, purchases ->
        // To be implemented in a later section.
    }

    private var billingClient: BillingClient? = null

    private fun ConflatedBroadcastChannel<BillingData>.update(block: (BillingData) -> BillingData) {
        val new = block(valueOrNull ?: BillingData())
        offer(new)
        Timber.i("Updated: $new")
    }

    fun initBilling(context: Context) {
        if (billingClient == null) {
            Timber.i("Initializing billing")
            billingClient = BillingClient.newBuilder(context)
                .setListener(purchasesUpdateListener)
                .enablePendingPurchases()
                .build()

            billingClient?.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    Timber.i("Billing connected")
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        // The BillingClient is ready. You can query purchases here.
                        updateSku()
                        billingChannel.update {
                            it.copy(billingAvailable = true)
                        }
                    }
                }

                override fun onBillingServiceDisconnected() {
                    Timber.i("Billing disconnected")
                    billingClient = null
                    billingChannel.update {
                        it.copy(billingAvailable = false)
                    }
                }
            })
        }
    }

    private fun updateSku() = GlobalScope.launch(Dispatchers.IO) {
        try {
            Timber.i("Updating SKU")

            Tasks.await(Firebase.remoteConfig.fetchAndActivate())
            val subscriptionSkuIds = Firebase.remoteConfig.getString("available_subscription_sku_id")
            val purchaseSkuIds = Firebase.remoteConfig.getString("available_purchase_sku_id")
            fun String.splitSkuIds() = split(",").map { it.trim() }
            Timber.i("Fetching SKU: subscriptions=$subscriptionSkuIds purchases=$purchaseSkuIds")

            val subscriptions = async {
                fetchSku(
                    SkuDetailsParams.newBuilder()
                        .setSkusList(subscriptionSkuIds.splitSkuIds())
                        .setType(BillingClient.SkuType.SUBS)
                        .build()
                )
            }

            val purchases = async {
                fetchSku(
                    SkuDetailsParams.newBuilder()
                        .setSkusList(purchaseSkuIds.splitSkuIds())
                        .setType(BillingClient.SkuType.INAPP)
                        .build()
                )
            }

            val allSku = listOf(subscriptions.await(), purchases.await()).flatten()
            Timber.i("Updated SKU: $allSku")
            billingChannel.update { it.copy(availableSku = allSku) }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    private suspend fun fetchSku(params: SkuDetailsParams): List<SkuDetails> {
        if (params.skusList.isEmpty()) {
            return emptyList()
        }

        val result = billingClient?.querySkuDetails(params)
        if (result?.billingResult?.responseCode == BillingClient.BillingResponseCode.OK) {
            return result.skuDetailsList ?: emptyList()
        } else {
            throw Exception("SKU update failed for $params: ${result?.billingResult?.debugMessage} (${result?.billingResult?.responseCode})")
        }
    }

    fun billingFlow(context: Context) = billingChannel.asFlow().onStart {
        initBilling(context)
    }
}