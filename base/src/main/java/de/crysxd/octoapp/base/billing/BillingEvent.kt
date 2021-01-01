package de.crysxd.octoapp.base.billing

sealed class BillingEvent {
    private var isConsumed = false

    fun consume(block: (BillingEvent) -> Unit) {
        if (!isConsumed) {
            block(this)
            isConsumed = true
        }
    }

    object PurchaseCompleted : BillingEvent()
}