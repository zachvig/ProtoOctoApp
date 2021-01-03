package de.crysxd.octoapp.base.billing

sealed class BillingEvent {
    private var isConsumed = false
    private val createdAt = System.currentTimeMillis()

    fun consume(block: (BillingEvent) -> Unit) {
        if (!isConsumed) {
            block(this)
            isConsumed = true
        }
    }

    fun isRecent() = (System.currentTimeMillis() - createdAt) < 1000

    object PurchaseCompleted : BillingEvent()
}