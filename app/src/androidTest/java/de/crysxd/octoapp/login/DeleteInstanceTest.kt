package de.crysxd.octoapp.login

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.hasSibling
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import com.adevinta.android.barista.rule.BaristaRule
import com.adevinta.android.barista.rule.flaky.AllowFlaky
import com.nhaarman.mockitokotlin2.verify
import de.crysxd.octoapp.MainActivity
import de.crysxd.octoapp.R
import de.crysxd.octoapp.base.billing.BillingManager
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformationV3
import de.crysxd.octoapp.base.ui.common.OctoTextInputLayout
import de.crysxd.octoapp.framework.SignInRobot
import de.crysxd.octoapp.framework.rules.MockDiscoveryRule
import de.crysxd.octoapp.framework.rules.ResetDaggerRule
import de.crysxd.octoapp.framework.rules.SpyOctoPrintRepositoryRule
import de.crysxd.octoapp.framework.rules.TestDocumentationRule
import de.crysxd.octoapp.framework.waitFor
import de.crysxd.octoapp.framework.waitForNot
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

class DeleteInstanceTest {

    private val spyOctoPrintRepositoryRule = SpyOctoPrintRepositoryRule()
    private val baristaRule = BaristaRule.create(MainActivity::class.java)
    private val discoveryRule = MockDiscoveryRule()

    @get:Rule
    val chain = RuleChain.outerRule(baristaRule)
        .around(TestDocumentationRule())
        .around(ResetDaggerRule())
        .around(discoveryRule)
        .around(spyOctoPrintRepositoryRule)

    private val instance = OctoPrintInstanceInformationV3(
        id = "random",
        webUrl = "http://test".toHttpUrl(),
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


    @Test(timeout = 30_000L)
    @AllowFlaky(attempts = 3)
    fun WHEN_a_instance_is_deleted_and_options_are_discovered_THEN_it_is_removed() {
        discoveryRule.mockForRandomFound()

        performDelete()

        // We stay on manual
        onView(withText(R.string.sign_in___discovery___previously_connected_devices)).check(matches(not(isDisplayed())))
        SignInRobot.scrollUp()
        onView(withText(R.string.sign_in___discovery___options_title)).check(matches(isDisplayed()))
    }

    @Test(timeout = 30_000L)
    @AllowFlaky(attempts = 3)
    fun WHEN_a_instance_is_deleted_and_nothing_is_discovered_THEN_it_is_removed() {
        discoveryRule.mockForNothingFound()

        performDelete()

        // Nothing else to be shown on options page, we move to manual
        onView(withText(R.string.sign_in___discovery___connect_manually_title)).check(matches(isDisplayed()))
        onView(allOf(withId(R.id.input), isAssignableFrom(OctoTextInputLayout::class.java))).check(matches(isDisplayed()))
    }

    @Test(timeout = 30_000L)
    @AllowFlaky(attempts = 3)
    fun WHEN_a_instance_is_deleted_and_quick_switch_disabled_THEN_it_is_removed() {
        BillingManager.enabledForTest = false
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
        baristaRule.launchActivity()

        // Wait for loading done and show options
        waitFor(
            viewMatcher = allOf(withId(R.id.title), isDisplayed(), withText(R.string.sign_in___discovery___options_title)),
            timeout = 5_000
        )

        // Check shown
        SignInRobot.scrollDown()
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
        verify(repository).remove(instance.id)
        waitForNot(allOf(withText(instance.label), isDisplayed()))
    }
}