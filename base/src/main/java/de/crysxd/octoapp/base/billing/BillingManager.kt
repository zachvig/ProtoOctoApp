package de.crysxd.octoapp.base.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import com.google.android.gms.tasks.Tasks
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import timber.log.Timber

@Suppress("EXPERIMENTAL_API_USAGE")
object BillingManager {

    private val pendingPurchases = mutableListOf<Purchase>()
    private val billingEventChannel = ConflatedBroadcastChannel<BillingEvent>()
    private val billingChannel = ConflatedBroadcastChannel(BillingData())
    private val purchasesUpdateListener = PurchasesUpdatedListener { billingResult, purchases ->
        Timber.i("On purchase updated: $billingResult $purchases")
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            GlobalScope.launch {
                handlePurchases(purchases)
            }
        } else if (billingResult.responseCode != BillingClient.BillingResponseCode.USER_CANCELED) {
            logError("Purchase flow failed", billingResult)
        }
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
                        queryPurchases()
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
            logError("SKU update failed for $params", result?.billingResult)
            return emptyList()
        }
    }

    fun billingFlow() = billingChannel.asFlow().distinctUntilChanged()
    fun billingEventFlow() = billingEventChannel.asFlow()

    fun purchase(activity: Activity, skuDetails: SkuDetails): Boolean {
        val flowParams = BillingFlowParams.newBuilder()
            .setSkuDetails(skuDetails)
            .build()

        val billingResult = billingClient?.launchBillingFlow(activity, flowParams)
        return if (billingResult?.responseCode == BillingClient.BillingResponseCode.OK) {
            true
        } else {
            logError("Unable to launch billing flow", billingResult)
            false
        }
    }

    private suspend fun handlePurchases(purchases: List<Purchase>): Unit {
        Timber.i("Handling ${purchases.size} purchases")
        try {
            purchases.forEach { purchase ->
                var sendPurchaseEvent = false
                if (!purchase.isAcknowledged) {
                    val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                    val billingResult = withContext(Dispatchers.IO) {
                        billingClient!!.acknowledgePurchase(acknowledgePurchaseParams.build())
                    }
                    if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                        logError("Failed to acknowledge purchase ${purchase.orderId}", billingResult)
                    } else {
                        Timber.i("Confirmed purchase ${purchase.orderId}")
                    }
                    sendPurchaseEvent = true
                }

                if (sendPurchaseEvent) {
                    billingEventChannel.offer(BillingEvent.PurchaseCompleted)
                }
            }

            val premiumActive = purchases.any {
                Purchase.PurchaseState.PURCHASED == it.purchaseState
            }
            billingChannel.update {
                it.copy(isPremiumActive = premiumActive)
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    private fun reattemptPurchaseHandling(purchases: List<Purchase>) {
        Timber.i("Reattempting purchase handling")
        pendingPurchases.clear()
        pendingPurchases.addAll(purchases)
    }

    private fun logError(description: String, billingResult: BillingResult?) {
        Timber.e(Exception("$description. responseCode=${billingResult?.responseCode} message=${billingResult?.debugMessage}"))
    }

    private fun queryPurchases() = GlobalScope.launch(Dispatchers.IO) {

        suspend fun queryPurchases(@BillingClient.SkuType type: String) {
            val purchaseResult = billingClient?.queryPurchases(type)
            if (purchaseResult?.billingResult?.responseCode != BillingClient.BillingResponseCode.OK) {
                logError("Unable to query purchases", purchaseResult?.billingResult)
            } else purchaseResult.purchasesList?.let {
                handlePurchases(it)
            }
        }

        try {
            if (billingClient?.isReady == true) {
                Timber.i("Querying purchases")
                queryPurchases(BillingClient.SkuType.INAPP)
                queryPurchases(BillingClient.SkuType.SUBS)
            } else {
                Timber.i("Billing client not ready, skipping purchase query")
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    fun onResume() = GlobalScope.launch {
        Timber.i("Resuming billing")

        if (pendingPurchases.isNotEmpty()) {
            handlePurchases(pendingPurchases)
            pendingPurchases.clear()
        }

        queryPurchases()
    }
}