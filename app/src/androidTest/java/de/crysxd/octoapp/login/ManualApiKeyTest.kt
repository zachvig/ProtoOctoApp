package de.crysxd.octoapp.login

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.filters.LargeTest
import com.adevinta.android.barista.rule.BaristaRule
import com.adevinta.android.barista.rule.flaky.AllowFlaky
import de.crysxd.octoapp.MainActivity
import de.crysxd.octoapp.R
import de.crysxd.octoapp.framework.SignInRobot
import de.crysxd.octoapp.framework.TestEnvironmentLibrary
import de.crysxd.octoapp.framework.rules.AcceptAllAccessRequestRule
import de.crysxd.octoapp.framework.rules.IdleTestEnvironmentRule
import de.crysxd.octoapp.framework.rules.MockDiscoveryRule
import de.crysxd.octoapp.framework.rules.ResetDaggerRule
import de.crysxd.octoapp.framework.rules.TestDocumentationRule
import de.crysxd.octoapp.framework.waitFor
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

@LargeTest
class ManualApiKeyTest {

    private val testEnv = TestEnvironmentLibrary.Terrier
    private val baristaRule = BaristaRule.create(MainActivity::class.java)
    private val discoveryRule = MockDiscoveryRule()

    @get:Rule
    val chain = RuleChain.outerRule(baristaRule)
        .around(IdleTestEnvironmentRule(testEnv))
        .around(TestDocumentationRule())
        .around(ResetDaggerRule())
        .around(discoveryRule)
        .around(AcceptAllAccessRequestRule(testEnv))

    @Test(timeout = 60_000L)
    @AllowFlaky(attempts = 3)
    fun WHEN_manually_entering_API_key_THEN_login_succeeds() = runBlocking {
        // GIVEN
        discoveryRule.mockForNothingFound()
        baristaRule.launchActivity()

        // Check loading
        SignInRobot.waitForWelcomeTitleToBeShown()

        // Wait for loading done and move to manual
        SignInRobot.waitForManualToBeShown()

        // Enter web URL and continue
        SignInRobot.manualInput.perform(ViewActions.replaceText(testEnv.webUrl.toString()))
        SignInRobot.continueButton.perform(click())

        // Wait for checks to pass
        SignInRobot.waitForRequestAccessToBeShown()

        // Move to manual API key flow
        onView(withText(R.string.sign_in___access___use_api_key)).perform(click())
        waitFor(allOf(withText(R.string.sign_in___manual_api_key___title), isDisplayed()))

        // Enter API key
        SignInRobot.manualInput.perform(ViewActions.replaceText(testEnv.apiKey))
        SignInRobot.continueButton.perform(click())

        // Wait for success (skip request access because we use the manual API key route)
        SignInRobot.waitForSignInToBeCompleted(skipRequestAccessCheck = true)
    }
}
