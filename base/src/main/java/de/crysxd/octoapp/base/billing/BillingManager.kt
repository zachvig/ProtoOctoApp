package de.crysxd.octoapp.base.billing

import android.app.Activity
import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.core.os.bundleOf
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.SkuDetails
import com.android.billingclient.api.SkuDetailsParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryPurchasesAsync
import com.android.billingclient.api.querySkuDetails
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.tasks.Tasks
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.base.di.Injector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber


@Suppress("EXPERIMENTAL_API_USAGE")
object BillingManager {

    const val FEATURE_AUTOMATIC_LIGHTS = "auto_lights"
    const val FEATURE_QUICK_SWITCH = "quick_switch"
    const val FEATURE_GCODE_PREVIEW = "gcode_preview"
    const val FEATURE_HLS_WEBCAM = "hls_webcam"
    const val FEATURE_INFINITE_WIDGETS = "infinite_app_widgets"
    const val FEATURE_FULL_WEBCAM_RESOLUTION = "full_webcam_resolution"

    @VisibleForTesting
    var enabledForTest: Boolean? = null

    private val billingEventChannel = ConflatedBroadcastChannel<BillingEvent>()
    private val billingChannel = ConflatedBroadcastChannel(BillingData())
    private val purchasesUpdateListener = PurchasesUpdatedListener { billingResult, purchases ->
        Timber.i("On purchase updated: $billingResult $purchases")
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                OctoAnalytics.logEvent(OctoAnalytics.Event.PurchaseFlowCompleted, bundleOf("sku" to purchases?.map { it.skus }?.joinToString(",")))
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
        Timber.v("Updated: $new")
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
            Timber.i("Updated SKU: ${allSku.map { it.sku }}")
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

    private suspend fun handlePurchases(purchases: List<Purchase>) {
        Timber.i("Handling ${purchases.size} purchases")
        try {
            // Check if premium is active
            val premiumActive = purchases.any {
                Purchase.PurchaseState.PURCHASED == it.purchaseState
            }
            val fromSubscription = purchases.any {
                Purchase.PurchaseState.PURCHASED == it.purchaseState && it.skus.any { sku -> sku.contains("_sub_") }
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
        if (billingResult == null) {
            Timber.w("Billing result is null, indicating billing connection was paused during an active process")
            return
        }

        if (billingResult.responseCode == 2) {
            Timber.w("No internet connection, service unavailable")
            return
        }

        if (billingResult.responseCode == -1) {
            Timber.w("Service was disconnected")
            return
        }

        val playServicesAvailable = try {
            val googleApiAvailability = GoogleApiAvailability.getInstance()
            val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(Injector.get().context())
            resultCode == ConnectionResult.SUCCESS
        } catch (e: java.lang.Exception) {
            Timber.e(e)
            null
        }

        if (playServicesAvailable != false) {
            Timber.e(Exception("$description. responseCode=${billingResult.responseCode} message=${billingResult.debugMessage} billingResult=${billingResult.let { "non-null" }} playServicesAvailable=$playServicesAvailable"))
        } else {
            Timber.w("BillingManager encountered problem but Play Services are not available")
        }
    }

    private fun queryPurchases() = GlobalScope.launch(Dispatchers.IO) {
        suspend fun queryPurchases(@BillingClient.SkuType type: String): List<Purchase> {
            val purchaseResult = billingClient?.queryPurchasesAsync(type)
            return if (purchaseResult?.billingResult?.responseCode != BillingClient.BillingResponseCode.OK) {
                logError("Unable to query purchases", purchaseResult?.billingResult)
                emptyList()
            } else {
                purchaseResult.purchasesList
            }
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

    fun isFeatureEnabled(feature: String): Boolean {
        val isPremiumFeature = Firebase.remoteConfig.getString("premium_features").split(",").map { it.trim() }.contains(feature)
        val hasPremium = billingChannel.valueOrNull?.isPremiumActive == true
        return enabledForTest ?: (!isPremiumFeature || hasPremium)
    }

    fun shouldAdvertisePremium() = billingChannel.valueOrNull?.let {
        it.isBillingAvailable && !it.isPremiumActive && it.availableSku.isNotEmpty()
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