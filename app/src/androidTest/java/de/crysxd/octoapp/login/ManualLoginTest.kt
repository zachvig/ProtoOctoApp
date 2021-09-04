package de.crysxd.octoapp.login

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.clearText
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.pressBack
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.filters.LargeTest
import de.crysxd.octoapp.R
import de.crysxd.octoapp.framework.SignInRobot
import de.crysxd.octoapp.framework.SignInRobot.continueButton
import de.crysxd.octoapp.framework.SignInRobot.manualInput
import de.crysxd.octoapp.framework.SignInRobot.waitForChecksToFailWithUnableToResolveHost
import de.crysxd.octoapp.framework.SignInRobot.waitForDiscoveryOptionsToBeShown
import de.crysxd.octoapp.framework.SignInRobot.waitForManualToBeShown
import de.crysxd.octoapp.framework.SignInRobot.waitForWelcomeTitleToBeShown
import de.crysxd.octoapp.framework.TestEnvironmentLibrary
import de.crysxd.octoapp.framework.rules.AcceptAllAccessRequestRule
import de.crysxd.octoapp.framework.rules.IdleTestEnvironmentRule
import de.crysxd.octoapp.framework.rules.LazyMainActivityScenarioRule
import de.crysxd.octoapp.framework.rules.MockDiscoveryRule
import de.crysxd.octoapp.framework.rules.MockTestFullNetworkStackRule
import de.crysxd.octoapp.framework.waitFor
import de.crysxd.octoapp.framework.waitForDialog
import de.crysxd.octoapp.framework.waitTime
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test

@LargeTest
class ManualLoginTest {

    private val testEnv = TestEnvironmentLibrary.Terrier

    @get:Rule
    val activityRule = LazyMainActivityScenarioRule()

    @get:Rule
    val discoveryRule = MockDiscoveryRule()

    @get:Rule
    val acceptAccessRequestRule = AcceptAllAccessRequestRule(testEnv)

    @get:Rule
    val mockTestFullNetworkStackRule = MockTestFullNetworkStackRule()

    @get:Rule
    val idleRule = IdleTestEnvironmentRule(testEnv)

    @Test(timeout = 60_000L)
    fun WHEN_no_instances_are_found_THEN_we_directly_move_to_manual_and_can_sign_in() = runBlocking {
        // GIVEN
        discoveryRule.mockForNothingFound()
        activityRule.launch()

        // Check loading
        waitForWelcomeTitleToBeShown()

        // Wait for loading done and move to manual
        waitForManualToBeShown()

        // Enter text and
        // Enter random web url (without http)
        val domain = "somelocaldomain.local"
        manualInput.perform(replaceText(domain))
        continueButton.perform(click())

        // Wait for checks to fail
        waitForChecksToFailWithUnableToResolveHost(domain)
    }

    @Test(timeout = 120_000L)
    fun WHEN_some_instances_are_found_THEN_we_can_still_move_to_manual() = runBlocking {
        // GIVEN
        discoveryRule.mockForRandomFound()
        activityRule.launch()

        // Check loading and loaded
        waitForWelcomeTitleToBeShown()
        waitForDiscoveryOptionsToBeShown()

        // Move to manual
        SignInRobot.scrollDown()
        onView(withId(R.id.manualConnectOption)).perform(click())

        // Wait for manual shown
        waitForManualToBeShown()

        // Go back, wait a bit for the new back press handler to settle...
        waitTime(100)
        onView(isRoot()).perform(closeSoftKeyboard())
        onView(isRoot()).perform(pressBack())

        // Move to manual
        waitFor(allOf(withText(R.string.sign_in___discovery___discovered_devices), isDisplayed()))
        SignInRobot.scrollDown()
        onView(withId(R.id.manualConnectOption)).perform(click())

        // Enter empty URL
        waitForManualToBeShown()
        manualInput.perform(clearText())
        continueButton.perform(click())
        waitForDialog(
            viewMatcher = allOf(
                isDisplayed(),
                withText(R.string.sign_in___discovery___error_invalid_url),
            )
        )
        onView(withText(android.R.string.ok)).perform(click())

        // Enter random web url (without http)
        val domain = "randomdomain.local"
        manualInput.perform(replaceText(domain))
        mockTestFullNetworkStackRule.mockLocalForDnsFailure()
        continueButton.perform(click())

        // Wait for checks to fail
        waitForChecksToFailWithUnableToResolveHost(domain)

        // Go back
        SignInRobot.scrollDown()
        onView(withText(R.string.sign_in___probe___edit_information)).perform(click())

        // Check text prefilled and start again
        waitForManualToBeShown()
        manualInput.check(matches(withText("http://$domain")))
        mockTestFullNetworkStackRule.mockLocalForDnsFailure()
        continueButton.perform(click())

        // Wait for checks to fail and go back
        waitForChecksToFailWithUnableToResolveHost(domain)
        onView(isRoot()).perform(pressBack())
        onView(withText(R.string.sign_in___cancel_and_use_other_information_title)).inRoot(isDialog()).perform(click())

        // Check text prefilled, correct and start again
        waitForManualToBeShown()
        manualInput.check(matches(withText("http://$domain")))
        manualInput.perform(replaceText(testEnv.webUrl))
        mockTestFullNetworkStackRule.mockForInvalidApiKey()
        continueButton.perform(click())

        SignInRobot.waitForChecks()
        SignInRobot.waitForSignInToBeCompleted()
    }
}
