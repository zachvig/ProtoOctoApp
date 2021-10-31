package de.crysxd.octoapp.login

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import com.adevinta.android.barista.rule.BaristaRule
import com.adevinta.android.barista.rule.flaky.AllowFlaky
import de.crysxd.octoapp.MainActivity
import de.crysxd.octoapp.R
import de.crysxd.octoapp.base.data.models.OctoPrintInstanceInformationV3
import de.crysxd.octoapp.base.di.BaseInjector
import de.crysxd.octoapp.framework.SignInRobot
import de.crysxd.octoapp.framework.rules.MockDiscoveryRule
import de.crysxd.octoapp.framework.rules.ResetDaggerRule
import de.crysxd.octoapp.framework.rules.TestDocumentationRule
import de.crysxd.octoapp.framework.waitForDialog
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.hamcrest.Matchers.startsWith
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

class BadSslTest {

    private val testUrl = "https://self-signed.badssl.com/".toHttpUrl()
    private val baristaRule = BaristaRule.create(MainActivity::class.java)

    @get:Rule
    val chain = RuleChain.outerRule(baristaRule)
        .around(TestDocumentationRule())
        .around(ResetDaggerRule())
        .around(MockDiscoveryRule())


    @Test(timeout = 60_000)
    @AllowFlaky(attempts = 3)
    fun WHEN_signing_in_with_bad_ssl_THEN_we_can_bypass_checks() {
        // GIVEN
        baristaRule.launchActivity()

        // Start sign in
        SignInRobot.waitForManualToBeShown()
        SignInRobot.manualInput.perform(ViewActions.replaceText(testUrl.toString()))
        SignInRobot.continueButton.perform(click())

        // Wait for basic auth form to be shown
        SignInRobot.waitForChecksToFailWithSslError(testUrl.host)
        SignInRobot.scrollDown()
        onView(withText(R.string.sing_in___probe___trust_and_continue)).perform(click())

        // Wait for checks to fail with 404
        SignInRobot.waitForChecksToFailWithOctoPrintNotFound()
    }

    @Test(timeout = 60_000)
    @AllowFlaky(attempts = 3)
    fun WHEN_ssl_certificate_becomes_invalid_THEN_user_can_trust_it() {
        // GIVEN
        BaseInjector.get().octorPrintRepository().setActive(
            OctoPrintInstanceInformationV3(
                id = "random",
                webUrl = testUrl,
                apiKey = "random,not used"
            )
        )
        baristaRule.launchActivity()

        // Wait for error
        val text = InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.sign_in___broken_setup___https_issue).takeWhile { it != '<' }
        waitForDialog(withText(startsWith(text)))
        onView(withText(R.string.sign_in___continue)).inRoot(RootMatchers.isDialog()).perform(click())

        // Wait for shown again and verify prefilled
        SignInRobot.waitForChecksToFailWithSslError(testUrl.host)
        SignInRobot.scrollDown()
        onView(withText(R.string.sing_in___probe___trust_and_continue)).perform(click())

        // Wait for checks to fail with 404
        SignInRobot.waitForChecksToFailWithOctoPrintNotFound()
    }
}