package de.crysxd.octoapp.base.billing

import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails

data class BillingData(
    val isBillingAvailable: Boolean = false,
    val isPremiumActive: Boolean = false,
    val isPremiumFromSubscription: Boolean = false,
    val purchases: Set<String> = emptySet(),
    val rawPurchases: Map<String, Purchase> = emptyMap(),
    val allSku: List<SkuDetails> = emptyList(),
)