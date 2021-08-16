package de.crysxd.octoapp.login

import android.content.Intent
import android.widget.EditText
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.clearText
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.pressBack
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import de.crysxd.octoapp.MainActivity
import de.crysxd.octoapp.R
import de.crysxd.octoapp.base.ui.common.OctoTextInputLayout
import de.crysxd.octoapp.framework.TestEnvironmentLibrary
import de.crysxd.octoapp.framework.rules.AcceptAllAccessRequestRule
import de.crysxd.octoapp.framework.rules.LazyActivityScenarioRule
import de.crysxd.octoapp.framework.rules.MockDiscoveryRule
import de.crysxd.octoapp.framework.waitFor
import de.crysxd.octoapp.framework.waitForDialog
import de.crysxd.octoapp.framework.waitTime
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test

@LargeTest
class ManualLoginTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val testEnv = TestEnvironmentLibrary.Terrier
    private val manualInput get() = onView(allOf(withId(R.id.input), isAssignableFrom(EditText::class.java))).check(matches(isDisplayed()))
    private val continueButton get() = onView(withText(R.string.sign_in___continue))

    @get:Rule
    val activityRule = LazyActivityScenarioRule<MainActivity>(launchActivity = false) {
        Intent(InstrumentationRegistry.getInstrumentation().targetContext, MainActivity::class.java)
    }

    @get:Rule
    val discoveryRule = MockDiscoveryRule()

    @get:Rule
    val acceptAccessRequestRule = AcceptAllAccessRequestRule(testEnv)

    @Test(timeout = 300_000L)
    fun WHEN_no_instances_are_found_THEN_we_directly_move_to_manual_and_can_sign_in() = runBlocking {
        // GIVEN
        discoveryRule.mockForNothingFound()
        activityRule.launch()

        // Check loading
        onView(allOf(withId(R.id.title), withText(R.string.sign_in___discovery___welcome_title)))
            .check(matches(isDisplayed()))

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

    @Test(timeout = 300_000L)
    fun WHEN_some_instances_are_found_THEN_we_can_still_move_to_manual() = runBlocking {
        // GIVEN
        discoveryRule.mockForRandomFound()
        activityRule.launch()

        // Check loading
        onView(withId(R.id.title))
            .check(matches(isDisplayed()))
            .check(matches(withText(R.string.sign_in___discovery___welcome_title)))

        // Wait for loading done and show options
        waitFor(
            viewMatcher = allOf(
                withId(R.id.title),
                isDisplayed(),
                withText(R.string.sign_in___discovery___options_title)
            ),
            timeout = 5_000
        )

        // Move to manual
        onView(withId(R.id.scrollView)).perform(swipeUp())
        onView(withId(R.id.manualConnectOption)).perform(click())

        // Wait for manual shown
        waitForManualToBeShown()

        // Go back, wait a bit for the new back press handler to settle...
        waitTime(100)
        onView(isRoot()).perform(closeSoftKeyboard())
        onView(isRoot()).perform(pressBack())

        // Move to manual
        waitFor(allOf(withText(R.string.sign_in___discovery___discovered_devices), isDisplayed()))
        onView(withId(R.id.scrollView)).perform(swipeUp())
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
        continueButton.perform(click())

        // Wait for checks to fail
        waitForChecksToFailWithUnableToResolveHost(domain)

        // Go back
        onView(withId(R.id.scrollView)).perform(swipeUp())
        onView(withText(R.string.sign_in___probe___edit_information)).perform(click())

        // Check text prefilled and start again
        waitForManualToBeShown()
        manualInput.check(matches(withText("http://$domain")))
        continueButton.perform(click())

        // Wait for checks to fail and go back
        waitForChecksToFailWithUnableToResolveHost(domain)
        onView(isRoot()).perform(pressBack())
        onView(withText(R.string.sign_in___cancel_and_use_other_information_title)).inRoot(isDialog()).perform(click())

        // Check text prefilled, correct and start again
        waitForManualToBeShown()
        manualInput.check(matches(withText("http://$domain")))
        manualInput.perform(replaceText(testEnv.webUrl))
        continueButton.perform(click())


        // Wait for checks
        waitFor(
            viewMatcher = allOf(
                withId(R.id.title),
                isDisplayed(),
                withText(R.string.sign_in___probe___probing_active_title)
            )
        )

        // Wait for access screen
        waitFor(
            timeout = 5000,
            viewMatcher = allOf(
                withId(R.id.title),
                isDisplayed(),
                withText(R.string.sign_in___access___confirm_in_web_interface)
            )
        )

        // Wait for success and continue
        waitFor(
            viewMatcher = allOf(
                withId(R.id.title),
                isDisplayed(),
                withText(R.string.sign_in___success___title)
            )
        )
        continueButton.perform(click())

        // Wait for connected screen
        waitFor(
            timeout = 5000,
            viewMatcher = allOf(
                isDisplayed(),
                withText(R.string.widget_temperature)
            )
        )
    }

    private fun waitForManualToBeShown() {
        waitFor(
            viewMatcher = allOf(
                withId(R.id.title),
                isDisplayed(),
                withText(R.string.sign_in___discovery___connect_manually_title)
            )
        )
        waitFor(
            viewMatcher = allOf(
                withId(R.id.input),
                isAssignableFrom(OctoTextInputLayout::class.java),
                isDisplayed(),
            )
        )
    }

    private fun waitForChecksToFailWithUnableToResolveHost(domain: String) {
        // Wait for checks to fail
        waitFor(
            viewMatcher = allOf(
                withId(R.id.title),
                isDisplayed(),
                withText(R.string.sign_in___probe___probing_active_title)
            )
        )
        waitFor(
            timeout = 5000,
            viewMatcher = allOf(
                withId(R.id.title),
                isDisplayed(),
                withText(context.getString(R.string.sign_in___probe_finding___title_local_dns_failure).replace("**%s**", domain)),
            )
        )
    }
}
