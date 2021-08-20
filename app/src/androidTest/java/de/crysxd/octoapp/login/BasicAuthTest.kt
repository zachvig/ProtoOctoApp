package de.crysxd.octoapp.login

import android.content.Intent
import android.net.Uri
import android.widget.EditText
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import de.crysxd.octoapp.MainActivity
import de.crysxd.octoapp.R
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.ext.urlEncode
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformationV2
import de.crysxd.octoapp.framework.SignInUtils
import de.crysxd.octoapp.framework.rules.LazyActivityScenarioRule
import de.crysxd.octoapp.framework.rules.MockDiscoveryRule
import de.crysxd.octoapp.framework.waitFor
import de.crysxd.octoapp.framework.waitForDialog
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test

class BasicAuthTest {

    private val testUrl = "https://jigsaw.w3.org/HTTP/Basic/"
    private val username = "guest"
    private val password = "guest"
    private val basicAuthFormMatcher = allOf(withId(R.id.usernameInput), isDisplayed())
    private val userNameInput = onView(allOf(isDescendantOfA(withId(R.id.usernameInput)), isAssignableFrom(EditText::class.java)))
    private val passwordInput = onView(allOf(isDescendantOfA(withId(R.id.passwordInput)), isAssignableFrom(EditText::class.java)))
    private val scrollView = onView(withId(R.id.scrollView))

    @get:Rule
    val activityRule = LazyActivityScenarioRule<MainActivity>(launchActivity = false) {
        Intent(InstrumentationRegistry.getInstrumentation().targetContext, MainActivity::class.java)
    }

    @get:Rule
    val discoveryRule = MockDiscoveryRule()

    @Test(timeout = 60_000)
    fun WHEN_signing_in_THEN_basic_auth_credentials_are_asked() {
        // GIVEN
        activityRule.launch()

        // Start sign in
        SignInUtils.waitForManualToBeShown()
        SignInUtils.manualInput.perform(replaceText(testUrl))
        SignInUtils.continueButton.perform(click())

        // Wait for basic auth form to be shown
        waitFor(basicAuthFormMatcher, timeout = 5000)

        // Enter wrong credentials
        val wrongUser = "secretuser$@&323"
        val wrongPassword = "secretpass$@&/?=§:;323"
        userNameInput.perform(replaceText(wrongUser))
        passwordInput.perform(replaceText(wrongPassword))
        scrollView.perform(swipeUp())
        SignInUtils.continueButton.perform(click())

        // Wait for shown again and verify prefilled
        waitFor(withText(R.string.sign_in___probe___probing_active_title))
        waitFor(basicAuthFormMatcher, timeout = 5000)
        userNameInput.check(matches(withText(wrongUser)))
        passwordInput.check(matches(withText(wrongPassword)))

        // Enter correct user
        userNameInput.perform(replaceText(username))
        passwordInput.perform(replaceText(password))
        scrollView.perform(swipeUp())
        SignInUtils.continueButton.perform(click())
        waitFor(withText(R.string.sign_in___probe___probing_active_title))

        // Check accepted (aka OctoPrint not found as we don't connect to a OctoPrint)
        waitFor(withText(R.string.sign_in___probe_finding___title_octoprint_not_found), timeout = 5000)
    }

    @Test(timeout = 60_000)
    fun WHEN_credentials_become_invalid_THEN_basic_auth_credentials_are_asked() {
        // GIVEN
        val wrongUser = "secretuser$@&323"
        val wrongPassword = "secretpass$@&/?=§:;323"
        val uri = Uri.parse(testUrl)
        val uriWithAuth = uri.buildUpon().encodedAuthority("${wrongUser.urlEncode()}:${wrongPassword.urlEncode()}@${uri.host}").build()
        Injector.get().octorPrintRepository().setActive(
            OctoPrintInstanceInformationV2(
                webUrl = uriWithAuth.toString(),
                apiKey = "random,not used"
            )
        )
        activityRule.launch()

        // Wait for error
        waitForDialog(withText(R.string.sign_in___broken_setup___basic_auth_required))
        onView(withText(R.string.sign_in___continue)).inRoot(isDialog()).perform(click())

        // Wait for shown again and verify prefilled
        waitFor(withText(R.string.sign_in___probe___probing_active_title))
        waitFor(basicAuthFormMatcher, timeout = 5_000)

        // Enter correct user
        userNameInput.perform(replaceText(username))
        passwordInput.perform(replaceText(password))
        scrollView.perform(swipeUp())
        SignInUtils.continueButton.perform(click())
        waitFor(withText(R.string.sign_in___probe___probing_active_title))

        // Check accepted (aka OctoPrint not found as we don't connect to a OctoPrint)
        waitFor(withText(R.string.sign_in___probe_finding___title_octoprint_not_found))
    }
}