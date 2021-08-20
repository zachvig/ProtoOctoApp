package de.crysxd.octoapp.framework

import android.widget.EditText
import androidx.annotation.StringRes
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.swipeDown
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import de.crysxd.octoapp.R
import de.crysxd.octoapp.base.ui.common.OctoTextInputLayout
import de.crysxd.octoapp.signin.discover.DiscoverOptionView
import org.hamcrest.Matchers.allOf

object SignInRobot {

    val manualInput
        get() = onView(allOf(withId(R.id.input), isAssignableFrom(EditText::class.java)))
            .check(matches(isDisplayed()))
    val continueButton get() = onView(withText(R.string.sign_in___continue))

    fun waitForWelcomeTitleToBeShown() {
        onView(
            allOf(
                withId(R.id.title),
                withText(R.string.sign_in___discovery___welcome_title)
            )
        ).check(matches(isDisplayed()))
    }

    fun waitForManualToBeShown() {
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

    fun waitForDiscoveryOptionsToBeShown() {
        waitFor(
            viewMatcher = allOf(
                withId(R.id.title),
                isDisplayed(),
                withText(R.string.sign_in___discovery___options_title)
            ),
            timeout = 5_000
        )
    }

    fun waitForChecks() {
        waitFor(
            viewMatcher = allOf(
                withId(R.id.title),
                isDisplayed(),
                withText(R.string.sign_in___probe___probing_active_title)
            )
        )
    }

    fun waitForChecksToFailWithUnableToResolveHost(domain: String) {
        // Wait for checks to fail
        waitForChecks()
        waitFor(
            timeout = 5000,
            viewMatcher = allOf(
                withId(R.id.title),
                isDisplayed(),
                withText(
                    InstrumentationRegistry.getInstrumentation().targetContext
                        .getString(R.string.sign_in___probe_finding___title_local_dns_failure)
                        .replace("**%s**", domain)
                ),
            )
        )
    }

    fun selectDiscoveryOptionWithText(text: String) {
        // Select discovered
        onView(
            allOf(
                isAssignableFrom(DiscoverOptionView::class.java),
                hasDescendant(withText(text))
            )
        ).perform(ViewActions.click())
    }

    fun selectDiscoveryOptionWithText(@StringRes text: Int) {
        // Select discovered
        onView(
            allOf(
                isAssignableFrom(DiscoverOptionView::class.java),
                hasDescendant(withText(text))
            )
        ).perform(ViewActions.click())
    }

    fun scrollDown() {
        onView(withId(R.id.scrollView)).perform(swipeUp())
    }

    fun scrollUp() {
        onView(withId(R.id.scrollView)).perform(swipeDown())
    }

    fun waitForSignInToBeCompleted(skipAccess: Boolean = false) {
        // Wait for access screen
        if (!skipAccess) {
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
            continueButton.perform(ViewActions.click())
        }

        // Wait for connected screen
        waitFor(
            timeout = 10_000,
            viewMatcher = allOf(
                isDisplayed(),
                withText(R.string.widget_temperature)
            )
        )
    }
}