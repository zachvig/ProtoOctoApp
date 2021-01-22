package de.crysxd.octoapp.base.billing

import android.app.Activity
import android.content.Context
import androidx.core.os.bundleOf
import com.android.billingclient.api.*
import com.google.android.gms.tasks.Tasks
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import de.crysxd.octoapp.base.OctoAnalytics
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import timber.log.Timber

@Suppress("EXPERIMENTAL_API_USAGE")
object BillingManager {

    private val billingEventChannel = ConflatedBroadcastChannel<BillingEvent>()
    private val billingChannel = ConflatedBroadcastChannel(BillingData())
    private val purchasesUpdateListener = PurchasesUpdatedListener { billingResult, purchases ->
        Timber.i("On purchase updated: $billingResult $purchases")
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                OctoAnalytics.logEvent(OctoAnalytics.Event.PurchaseFlowCompleted, bundleOf("sku" to purchases?.joinToString(",") { it.sku }))
                GlobalScope.launch {
                    purchases?.let { handlePurchases(it) }
                }
            }

            BillingClient.BillingResponseCode.USER_CANCELED -> OctoAnalytics.logEvent(OctoAnalytics.Event.PurchaseFlowCancelled)

            else -> {
                OctoAnalytics.logEvent(OctoAnalytics.Event.PurchaseFlowFailed)
                logError("Purchase flow failed", billingResult)
            }
        }
    }

    private var billingClient: BillingClient? = null

    private fun ConflatedBroadcastChannel<BillingData>.update(block: (BillingData) -> BillingData) {
        val new = block(valueOrNull ?: BillingData())
        offer(new)
        Timber.i("Updated: $new")
    }

    fun initBilling(context: Context) = GlobalScope.launch(Dispatchers.IO) {
        try {
            OctoAnalytics.setUserProperty(OctoAnalytics.UserProperty.BillingStatus, "unknown")
            fetchRemoteConfig()

            Timber.i("Initializing billing")
            billingClient?.endConnection()
            billingClient = BillingClient.newBuilder(context)
                .setListener(purchasesUpdateListener)
                .enablePendingPurchases()
                .build()

            billingClient?.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    Timber.i("Billing connected")
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        OctoAnalytics.setUserProperty(OctoAnalytics.UserProperty.BillingStatus, "available")
                        // The BillingClient is ready. You can query purchases here.
                        updateSku()
                        queryPurchases()
                        billingChannel.update {
                            it.copy(isBillingAvailable = Firebase.remoteConfig.getBoolean("billing_active"))
                        }
                    }
                }

                override fun onBillingServiceDisconnected() {
                    Timber.i("Billing disconnected")
                    billingClient = null
                    billingChannel.update {
                        it.copy(isBillingAvailable = false)
                    }
                }
            })
        } catch (e: Exception) {
            OctoAnalytics.setUserProperty(OctoAnalytics.UserProperty.BillingStatus, "error")
            Timber.e(e)
        }
    }

    private fun fetchRemoteConfig() {
        try {
            Timber.i("Fetching latest remote config")
            Tasks.await(Firebase.remoteConfig.fetchAndActivate())
        } catch (e: Exception) {
            // Continue with old values
        }
    }

    private fun updateSku() = GlobalScope.launch(Dispatchers.IO) {
        try {
            fetchRemoteConfig()
            Timber.i("Updating SKU")
            val subscriptionSkuIds = Firebase.remoteConfig.getString("available_subscription_sku_id")
            val purchaseSkuIds = Firebase.remoteConfig.getString("available_purchase_sku_id")
            fun String.splitSkuIds() = split(",").map { it.trim() }
            Timber.i("Fetching SKU: subscriptions=$subscriptionSkuIds purchases=$purchaseSkuIds")

            val supervisor = SupervisorJob()
            val subscriptions = async(supervisor) {
                fetchSku(
                    SkuDetailsParams.newBuilder()
                        .setSkusList(subscriptionSkuIds.splitSkuIds())
                        .setType(BillingClient.SkuType.SUBS)
                        .build()
                )
            }

            val purchases = async(supervisor) {
                fetchSku(
                    SkuDetailsParams.newBuilder()
                        .setSkusList(purchaseSkuIds.splitSkuIds())
                        .setType(BillingClient.SkuType.INAPP)
                        .build()
                )
            }

            val allSku = listOf(subscriptions.await(), purchases.await()).flatten()
            Timber.i("Updated SKU: $allSku")
            Timber.i("Premium features: ${Firebase.remoteConfig.getString("premium_features")}")
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
        return if (result?.billingResult?.responseCode == BillingClient.BillingResponseCode.OK) {
            result.skuDetailsList ?: emptyList()
        } else {
            logError("SKU update failed for $params", result?.billingResult)
            emptyList()
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
            OctoAnalytics.logEvent(OctoAnalytics.Event.PurchaseFlowFailed, bundleOf("code" to billingResult?.responseCode, "sku" to skuDetails.sku))
            logError("Unable to launch billing flow", billingResult)
            false
        }
    }

    private suspend fun handlePurchases(purchases: List<Purchase>): Unit {
        Timber.i("Handling ${purchases.size} purchases")
        try {
            // Check if premium is active
            val premiumActive = purchases.any {
                Purchase.PurchaseState.PURCHASED == it.purchaseState
            }
            val fromSubscription = purchases.any {
                Purchase.PurchaseState.PURCHASED == it.purchaseState && it.sku.contains("_sub_")
            }

            OctoAnalytics.setUserProperty(OctoAnalytics.UserProperty.PremiumUser, premiumActive.toString())
            OctoAnalytics.setUserProperty(OctoAnalytics.UserProperty.PremiumSubUser, fromSubscription.toString())

            billingChannel.update {
                it.copy(
                    isPremiumActive = premiumActive,
                    isPremiumFromSubscription = fromSubscription,
                    purchases = it.purchases.toMutableSet().apply {
                        addAll(purchases.map { purchase -> purchase.orderId })
                    }
                )
            }

            // Activate purchases
            var purchaseEventSent = false
            purchases.forEach { purchase ->
                if (!purchase.isAcknowledged) {
                    if (!purchaseEventSent) {
                        purchaseEventSent = true
                        billingEventChannel.offer(BillingEvent.PurchaseCompleted)
                    }

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
                }
            }


        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    private fun logError(description: String, billingResult: BillingResult?) {
        Timber.e(Exception("$description. responseCode=${billingResult?.responseCode} message=${billingResult?.debugMessage}"))
    }

    private fun queryPurchases() = GlobalScope.launch(Dispatchers.IO) {
        suspend fun queryPurchases(@BillingClient.SkuType type: String): List<Purchase> {
            val purchaseResult = billingClient?.queryPurchases(type)
            if (purchaseResult?.billingResult?.responseCode != BillingClient.BillingResponseCode.OK) {
                logError("Unable to query purchases", purchaseResult?.billingResult)
            } else purchaseResult.purchasesList?.let {
                return it
            }

            return emptyList()
        }

        try {
            if (billingClient?.isReady == true) {
                Timber.i("Querying purchases")
                val purchases = listOf(
                    queryPurchases(BillingClient.SkuType.INAPP),
                    queryPurchases(BillingClient.SkuType.SUBS)
                ).flatten()
                Timber.i("Found ${purchases.size} purchases")
                handlePurchases(purchases)
            } else {
                Timber.i("Billing client not ready, skipping purchase query")
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    fun isFeatureEnabled(feature: String) = !Firebase.remoteConfig.getString("premium_features")
        .split(",")
        .map {
            it.trim()
        }.contains(feature) || billingChannel.valueOrNull?.isPremiumActive == true

    fun shouldAdvertisePremium() = billingChannel.valueOrNull?.let {
        it.isBillingAvailable && !it.isPremiumActive
    } ?: false

    fun onResume(context: Context) = GlobalScope.launch {
        Timber.i("Resuming billing")
        initBilling(context)
    }

    fun onPause() {
        Timber.i("Pausing billing")
        billingClient?.endConnection()
        billingClient = null
    }
}