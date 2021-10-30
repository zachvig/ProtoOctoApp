package de.crysxd.octoapp.base.data.models

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import timber.log.Timber

data class Sale(
    val banner: String?,
    @SerializedName("sale_id") val saleId: String?,
    @SerializedName("end_time") val endTime: Long?,
    val offers: Map<String, String>?,
)

fun FirebaseRemoteConfig.getSale(): Sale? {
    try {
        return Gson().let {
            val json = getString("purchase_sale").takeIf { it.isNotBlank() } ?: return@getSale null
            it.fromJson(json, Sale::class.java)
        }
    } catch (e: Exception) {
        Timber.e(e)
        return null
    }
}