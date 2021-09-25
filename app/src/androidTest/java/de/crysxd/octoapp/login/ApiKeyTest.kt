package de.crysxd.octoapp.login

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.adevinta.android.barista.rule.BaristaRule
import com.adevinta.android.barista.rule.flaky.AllowFlaky
import de.crysxd.octoapp.MainActivity
import de.crysxd.octoapp.R
import de.crysxd.octoapp.base.di.BaseInjector
import de.crysxd.octoapp.framework.SignInRobot
import de.crysxd.octoapp.framework.TestEnvironmentLibrary
import de.crysxd.octoapp.framework.rules.AcceptAllAccessRequestRule
import de.crysxd.octoapp.framework.rules.IdleTestEnvironmentRule
import de.crysxd.octoapp.framework.rules.MockDiscoveryRule
import de.crysxd.octoapp.framework.rules.ResetDaggerRule
import de.crysxd.octoapp.framework.rules.TestDocumentationRule
import de.crysxd.octoapp.framework.waitForDialog
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain


class ApiKeyTest {

    private val testEnv = TestEnvironmentLibrary.Terrier
    private val baristaRule = BaristaRule.create(MainActivity::class.java)

    @get:Rule
    val chain = RuleChain.outerRule(baristaRule)
        .around(IdleTestEnvironmentRule(testEnv))
        .around(TestDocumentationRule())
        .around(ResetDaggerRule())
        .around(MockDiscoveryRule())
        .around(AcceptAllAccessRequestRule(testEnv))

    @Test(timeout = 30_000)
    @AllowFlaky(attempts = 3)
    fun WHEN_api_key_become_invalid_THEN_new_api_key_is_requested() {
        // GIVEN
        BaseInjector.get().octorPrintRepository().setActive(testEnv.copy(apiKey = "wrong"))
        baristaRule.launchActivity()

        // Wait for error
        waitForDialog(withText(R.string.sign_in___broken_setup___api_key_revoked))
        onView(withText(R.string.sign_in___continue)).inRoot(isDialog()).perform(click())

        // Wait test, access request, success, connected
        SignInRobot.waitForChecks()
        SignInRobot.waitForSignInToBeCompleted()
    }
}