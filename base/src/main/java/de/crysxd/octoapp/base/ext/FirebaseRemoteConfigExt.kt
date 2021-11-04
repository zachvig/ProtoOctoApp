package de.crysxd.octoapp.base.ext

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.gson.Gson
import de.crysxd.octoapp.base.billing.BillingManager
import de.crysxd.octoapp.base.data.models.PurchaseOffers
import timber.log.Timber

val FirebaseRemoteConfig.purchaseOffers: PurchaseOffers
    get() = if (BillingManager.shouldAdvertisePremium()) {
        purchaseOffersForced
    } else {
        PurchaseOffers.DEFAULT
    }


val FirebaseRemoteConfig.purchaseOffersForced: PurchaseOffers
    get() = try {
        val json = getString("purchase_offers").takeIf { it.isNotEmpty() }
        val m = Gson().fromJson(json, PurchaseOffers::class.java)
        // "Testing" the object to ensure everything is decoded correctly
        m.activeConfig.textsWithData.highlightBanner
        m.baseConfig.textsWithData.highlightBanner
        m.purchaseSku
        m.subscriptionSku
        m.saleConfigs
        m
    } catch (e: Exception) {
        Timber.e(e)
        null
    } ?: PurchaseOffers.DEFAULT
