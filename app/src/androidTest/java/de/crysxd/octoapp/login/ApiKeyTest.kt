package de.crysxd.octoapp.login

import android.content.Intent
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import de.crysxd.octoapp.MainActivity
import de.crysxd.octoapp.R
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.framework.TestEnvironmentLibrary
import de.crysxd.octoapp.framework.rules.AcceptAllAccessRequestRule
import de.crysxd.octoapp.framework.rules.LazyActivityScenarioRule
import de.crysxd.octoapp.framework.rules.MockDiscoveryRule
import de.crysxd.octoapp.framework.waitFor
import de.crysxd.octoapp.framework.waitForDialog
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test

class ApiKeyTest {

    private val testEnv = TestEnvironmentLibrary.Terrier

    @get:Rule
    val activityRule = LazyActivityScenarioRule<MainActivity>(launchActivity = false) {
        Intent(InstrumentationRegistry.getInstrumentation().targetContext, MainActivity::class.java)
    }

    @get:Rule
    val discoveryRule = MockDiscoveryRule()

    @get:Rule
    val acceptAccessRequestRule = AcceptAllAccessRequestRule(testEnv).also {
        it.instanceInformation = testEnv
    }


    @Test(timeout = 60_000)
    fun WHEN_api_key_become_invalid_THEN_new_api_key_is_requested() {
        // GIVEN
        Injector.get().octorPrintRepository().setActive(testEnv.copy(apiKey = "wrong"))
        activityRule.launch()

        // Wait for error
        waitForDialog(withText(R.string.sign_in___broken_setup___api_key_revoked))
        Espresso.onView(withText(R.string.sign_in___continue)).inRoot(isDialog()).perform(click())

        // Wait test, access request, success, connected
        waitFor(withText(R.string.sign_in___probe___probing_active_title), timeout = 5000)
        waitFor(withText(R.string.sign_in___access___open_web))
        waitFor(withText(R.string.sign_in___success___title))
        onView(withText(R.string.sign_in___continue)).perform(click())
        waitFor(allOf(ViewMatchers.isDisplayed(), ViewMatchers.withText(R.string.widget_temperature)), timeout = 5000)
    }
}