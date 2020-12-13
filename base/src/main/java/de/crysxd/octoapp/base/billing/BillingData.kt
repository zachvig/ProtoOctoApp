package de.crysxd.octoapp.base.billing

import com.android.billingclient.api.SkuDetails

data class BillingData(
    val billingAvailable: Boolean = false,
    val isPremiumActive: Boolean = false,
    val availableSku: List<SkuDetails> = emptyList(),
)