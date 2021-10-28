package de.crysxd.octoapp.base.billing

import org.junit.Assert.assertNull
import org.junit.Test

class BillingManagerTest {

    @Test
    fun `WHEN Billing is initialised THEN debug flag is null`() {
        assertNull(BillingManager.enabledForTest)
    }
}