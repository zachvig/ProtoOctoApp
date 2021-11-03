package de.crysxd.octoapp.base.data.models

import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

data class PurchaseOffers(
    val baseConfig: PurchaseConfig,
    val saleConfigs: List<PurchaseConfig>?,
    val purchaseSku: List<String>?,
    val subscriptionSku: List<String>?,
) {

    companion object {
        val DEFAULT = PurchaseOffers(
            baseConfig = PurchaseConfig(
                advertisement = null,
                offers = null,
                validFrom = null,
                validTo = null,
                texts = Texts(
                    highlightBanner = null,
                    purchaseScreenTitle = "Thank you for<br>supporting OctoApp!",
                    purchaseScreenContinueCta = "Support OctoApp",
                    purchaseScreenDescription = "Hi, I'm Chris! Creating OctoApp and supporting all of you takes a lot of my time. To be able to continue this effort, I decided to make more advanced features exclusive for my supporters.</u><br/><br/><b>I will never start showing ads in this app nor will I take away features that are already available.</b><br/><br/>Following features will be unlocked:",
                    skuListTitle = "High Eight! Thanks for your support! You rock!",
                    launchPurchaseScreenCta = "Support OctoApp",
                    purchaseScreenFeatures = "Multi-OctoPrint<br/>Gcode viewer<br/>HLS webcam streams<br/>Infinite widgets<br/>Automatic lights",
                    purchaseScreenMoreFeatures = "and much more to come!",
                    launchPurchaseScreenHighlight = null,
                )
            ),
            saleConfigs = null,
            purchaseSku = null,
            subscriptionSku = null,
        )
    }

    data class PurchaseConfig(
        val advertisement: Advertisement?,
        val offers: Map<String, Offer>?,
        val texts: Texts,
        val validFrom: String?,
        val validTo: String?,
    ) {
        companion object {
            private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH)
            private const val COUNTDOWN_PLACEHOLDER = "{{countdown}}"
        }

        val validFromDate get() = validFrom?.let(dateFormat::parse)
        val validToDate get() = validTo?.let(dateFormat::parse)

        private val countDown: String
            get() {
                val now = System.currentTimeMillis()
                val until = validToDate?.time ?: Long.MAX_VALUE
                val left = (until - now).coerceAtLeast(0)
                val days = TimeUnit.MILLISECONDS.toDays(left)
                val hours = TimeUnit.MILLISECONDS.toHours(left - TimeUnit.DAYS.toMillis(days))
                val minutes = TimeUnit.MILLISECONDS.toMinutes(left - TimeUnit.DAYS.toMillis(days) - TimeUnit.HOURS.toMillis(hours))
                val seconds = TimeUnit.MILLISECONDS.toSeconds(left - TimeUnit.DAYS.toMillis(days) - TimeUnit.HOURS.toMillis(hours) - TimeUnit.MINUTES.toMillis(minutes))
                return listOfNotNull(
                    String.format("%dd", days).takeIf { days > 0 },
                    String.format("%02dh", hours).takeIf { hours > 0 },
                    String.format("%02dm", minutes),
                    String.format("%02ds", seconds),
                ).joinToString(" ")
            }

        val textsWithData
            get() = texts.copy(
                highlightBanner = texts.highlightBanner?.replace(COUNTDOWN_PLACEHOLDER, countDown),
                launchPurchaseScreenHighlight = texts.launchPurchaseScreenHighlight?.replace(COUNTDOWN_PLACEHOLDER, countDown),
            )
        val advertisementWithData get() = advertisement?.copy(message = advertisement.message.replace(COUNTDOWN_PLACEHOLDER, countDown))
    }

    data class Advertisement(
        val id: String,
        val message: String,
    )

    data class Texts(
        val highlightBanner: String?,
        val purchaseScreenTitle: String,
        val purchaseScreenContinueCta: String,
        val purchaseScreenDescription: String,
        val purchaseScreenFeatures: String,
        val purchaseScreenMoreFeatures: String,
        val skuListTitle: String,
        val launchPurchaseScreenCta: String,
        val launchPurchaseScreenHighlight: String?,
    )

    data class Offer(
        val badge: Badge?,
        val dealFor: String?,
        val label: String?
    )

    enum class Badge {
        @SerializedName("popular")
        Popular,

        @SerializedName("best_value")
        BestValue,

        @SerializedName("sale")
        Sale
    }

    val activeConfig
        get() = saleConfigs?.firstOrNull {
            val from = it.validFromDate
            val to = it.validToDate
            from != null && to != null && System.currentTimeMillis() in from.time..to.time
        } ?: saleConfigs?.firstOrNull {
            val from = it.validFromDate
            val to = it.validToDate
            to == null && from != null && System.currentTimeMillis() > from.time
        } ?: baseConfig
}