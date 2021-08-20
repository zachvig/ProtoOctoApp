package de.crysxd.octoapp.login

import android.content.Intent
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.swipeDown
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.hasSibling
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import com.nhaarman.mockitokotlin2.verify
import de.crysxd.octoapp.MainActivity
import de.crysxd.octoapp.R
import de.crysxd.octoapp.base.billing.BillingManager
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformationV2
import de.crysxd.octoapp.base.ui.common.OctoTextInputLayout
import de.crysxd.octoapp.framework.rules.LazyActivityScenarioRule
import de.crysxd.octoapp.framework.rules.MockDiscoveryRule
import de.crysxd.octoapp.framework.rules.SpyOctoPrintRepositoryRule
import de.crysxd.octoapp.framework.waitFor
import de.crysxd.octoapp.framework.waitForNot
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class DeleteInstanceTest {

    @get:Rule
    val activityRule = LazyActivityScenarioRule<MainActivity>(launchActivity = false) {
        Intent(InstrumentationRegistry.getInstrumentation().targetContext, MainActivity::class.java)
    }

    @get:Rule
    val discoveryRule = MockDiscoveryRule()

    @get:Rule
    val spyOctoPrintRepositoryRule = SpyOctoPrintRepositoryRule()

    private val instance = OctoPrintInstanceInformationV2(
        webUrl = "http://test",
        apiKey = "none",
    )

    @Before
    fun setUp() {
        BillingManager.enabledForTest = true
    }

    @After
    fun tearDown() {
        BillingManager.enabledForTest = null
    }


    @Test
    fun WHEN_a_instance_is_deleted_and_options_are_discovered_THEN_it_is_removed() {
        discoveryRule.mockForRandomFound()

        performDelete()

        // We stay on manual
        onView(withText(R.string.sign_in___discovery___previously_connected_devices)).check(matches(not(isDisplayed())))
        onView(withId(R.id.scrollView)).perform(swipeDown())
        onView(withText(R.string.sign_in___discovery___options_title)).check(matches(isDisplayed()))
    }

    @Test
    fun WHEN_a_instance_is_deleted_and_nothing_is_discovered_THEN_it_is_removed() {
        discoveryRule.mockForNothingFound()

        performDelete()

        // Nothing else to be shown on options page, we move to manual
        onView(withText(R.string.sign_in___discovery___connect_manually_title)).check(matches(isDisplayed()))
        onView(allOf(withId(R.id.input), isAssignableFrom(OctoTextInputLayout::class.java))).check(matches(isDisplayed()))
    }

    private fun performDelete() {
        val repository = Injector.get().octorPrintRepository()
        repository.setActive(instance)
        repository.clearActive()
        activityRule.launch()

        // Wait for loading done and show options
        waitFor(
            viewMatcher = allOf(withId(R.id.title), isDisplayed(), withText(R.string.sign_in___discovery___options_title)),
            timeout = 5_000
        )

        // Check shown
        onView(withId(R.id.scrollView)).perform(swipeUp())
        val previousConnectedTitle = onView(withText(R.string.sign_in___discovery___previously_connected_devices))
        val instanceTitle = onView(withText(instance.label))
        previousConnectedTitle.check(matches(isDisplayed()))
        instanceTitle.check(matches(isDisplayed()))

        // Show delete options
        onView(withId(R.id.buttonShowDelete)).perform(click())

        // Delete option
        onView(allOf(hasSibling(hasDescendant(withText(instance.label))), withId(R.id.buttonDelete))).perform(click())

        // Confirmation dialog
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val text = context.getString(R.string.sign_in___discovery___delete_printer_message, instance.label)
        onView(withText(text)).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(withText(R.string.sign_in___discovery___delete_printer_confirmation)).inRoot(isDialog()).perform(click())

        // Check gone
        verify(repository).remove(instance.webUrl)
        waitForNot(allOf(withText(instance.label), isDisplayed()))
    }
}