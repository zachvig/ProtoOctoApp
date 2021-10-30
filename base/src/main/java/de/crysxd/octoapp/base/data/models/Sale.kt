package de.crysxd.octoapp.base.data.models

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import de.crysxd.octoapp.base.ext.toHtml
import timber.log.Timber
import java.util.concurrent.TimeUnit

data class Sale(
    val banner: String?,
    val bannerCompact: String?,
    @SerializedName("sale_id") val saleId: String?,
    @SerializedName("end_time") val endTime: Long?,
    @SerializedName("start_time") val startTime: Long?,
    val offers: Map<String, String>?,
) {
    val bannerWithTime: CharSequence?
        get() {
            val banner = banner ?: return null
            val until = endTime ?: 0L
            val now = System.currentTimeMillis()
            val left = (until - now).coerceAtLeast(0)
            val hours = TimeUnit.MILLISECONDS.toHours(left)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(left - TimeUnit.HOURS.toMillis(hours))
            val seconds = TimeUnit.MILLISECONDS.toSeconds(left - TimeUnit.HOURS.toMillis(hours) - TimeUnit.MINUTES.toMillis(minutes))
            val countDown = String.format("%02dh %02dmin %02ds", hours, minutes, seconds)
            return String.format(banner, countDown).toHtml()
        }
}

fun FirebaseRemoteConfig.getSale(): Sale? {
    try {
        return Gson().let {
            val json = getString("purchase_sale").takeIf { it.isNotBlank() } ?: return@getSale null
            Timber.d("Using sale: $json")
            it.fromJson(json, Sale::class.java)
        }.takeIf { (it.endTime == null || it.endTime > System.currentTimeMillis()) && (it.startTime == null || it.startTime < System.currentTimeMillis()) }
    } catch (e: Exception) {
        Timber.e(e)
        return null
    }
}