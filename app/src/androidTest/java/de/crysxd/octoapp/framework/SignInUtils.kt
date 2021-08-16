package de.crysxd.octoapp.framework

import android.widget.EditText
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.platform.app.InstrumentationRegistry
import de.crysxd.octoapp.R
import de.crysxd.octoapp.base.ui.common.OctoTextInputLayout
import org.hamcrest.Matchers

object SignInUtils {

    val manualInput
        get() = Espresso.onView(Matchers.allOf(ViewMatchers.withId(R.id.input), ViewMatchers.isAssignableFrom(EditText::class.java)))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    val continueButton get() = Espresso.onView(ViewMatchers.withText(R.string.sign_in___continue))

    fun waitForManualToBeShown() {
        waitFor(
            viewMatcher = Matchers.allOf(
                ViewMatchers.withId(R.id.title),
                ViewMatchers.isDisplayed(),
                ViewMatchers.withText(R.string.sign_in___discovery___connect_manually_title)
            )
        )
        waitFor(
            viewMatcher = Matchers.allOf(
                ViewMatchers.withId(R.id.input),
                ViewMatchers.isAssignableFrom(OctoTextInputLayout::class.java),
                ViewMatchers.isDisplayed(),
            )
        )
    }

    fun waitForChecksToFailWithUnableToResolveHost(domain: String) {
        // Wait for checks to fail
        waitFor(
            viewMatcher = Matchers.allOf(
                ViewMatchers.withId(R.id.title),
                ViewMatchers.isDisplayed(),
                ViewMatchers.withText(R.string.sign_in___probe___probing_active_title)
            )
        )
        waitFor(
            timeout = 5000,
            viewMatcher = Matchers.allOf(
                ViewMatchers.withId(R.id.title),
                ViewMatchers.isDisplayed(),
                ViewMatchers.withText(
                    InstrumentationRegistry.getInstrumentation().targetContext
                        .getString(R.string.sign_in___probe_finding___title_local_dns_failure)
                        .replace("**%s**", domain)
                ),
            )
        )
    }
}