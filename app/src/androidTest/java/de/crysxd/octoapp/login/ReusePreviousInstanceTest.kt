package de.crysxd.octoapp.login

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.pressBack
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import de.crysxd.octoapp.R
import de.crysxd.octoapp.base.billing.BillingManager
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.framework.SignInRobot
import de.crysxd.octoapp.framework.TestEnvironmentLibrary
import de.crysxd.octoapp.framework.rules.LazyMainActivityScenarioRule
import de.crysxd.octoapp.framework.rules.MockDiscoveryRule
import de.crysxd.octoapp.framework.rules.MockTestFullNetworkStackRule
import de.crysxd.octoapp.framework.waitFor
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ReusePreviousInstanceTest {

    private val testEnv = TestEnvironmentLibrary.Terrier

    @get:Rule
    val activityRule = LazyMainActivityScenarioRule()

    @get:Rule
    val discoveryRule = MockDiscoveryRule()

    @get:Rule
    val mockTestFullNetworkStackRule = MockTestFullNetworkStackRule()

    @Before
    fun setUp() {
        val repo = Injector.get().octorPrintRepository()
        repo.setActive(testEnv)
        repo.clearActive()
        discoveryRule.mockForRandomFound()
    }

    @After
    fun tearDown() {
        BillingManager.enabledForTest = null
    }

    @Test(timeout = 30_000L)
    fun WHEN_feature_disabled_THEN_purchase_flow_is_started() {
        BillingManager.enabledForTest = false
        activityRule.launch()

        SignInRobot.waitForDiscoveryOptionsToBeShown()
        SignInRobot.scrollDown()

        onView(withText(R.string.sign_in___discovery___previously_connected_devices)).check(matches(isDisplayed()))
        onView(withText(R.string.sign_in___discovery___quick_switch_disabled_title)).check(matches(isDisplayed()))
        onView(withText(R.string.sign_in___discovery___quick_switch_disabled_subtitle)).check(matches(isDisplayed()))

        // Select env...should start purchase flow
        SignInRobot.selectDiscoveryOptionWithText(testEnv.label)
        waitFor(allOf(withText(R.string.billing_unsupported_title), isDisplayed()))

        // Go back and select purchase item
        onView(isRoot()).perform(pressBack())
        SignInRobot.selectDiscoveryOptionWithText(R.string.sign_in___discovery___quick_switch_disabled_title)
        waitFor(allOf(withText(R.string.billing_unsupported_title), isDisplayed()))
    }

    @Test(timeout = 30_000L)
    fun WHEN_feature_enabled_THEN_sign_in_succeeds() {
        BillingManager.enabledForTest = true
        activityRule.launch()

        SignInRobot.waitForDiscoveryOptionsToBeShown()
        SignInRobot.scrollDown()
        SignInRobot.selectDiscoveryOptionWithText(testEnv.label)
        SignInRobot.waitForSignInToBeCompleted(skipAccess = true)

        assertThat(Injector.get().octorPrintRepository().getActiveInstanceSnapshot()?.webUrl).isEqualTo(testEnv.webUrl)
        verifyZeroInteractions(Injector.get().testFullNetworkStackUseCase())
    }
}