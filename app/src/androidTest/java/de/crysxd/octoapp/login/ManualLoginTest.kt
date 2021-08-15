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
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.whenever
import de.crysxd.octoapp.MainActivity
import de.crysxd.octoapp.R
import de.crysxd.octoapp.base.di.BaseComponent
import de.crysxd.octoapp.base.ui.common.OctoTextInputLayout
import de.crysxd.octoapp.base.usecase.DiscoverOctoPrintUseCase
import de.crysxd.octoapp.base.usecase.RequestApiAccessUseCase
import de.crysxd.octoapp.framework.LazyActivityScenarioRule
import de.crysxd.octoapp.framework.TestEnvironmentLibrary
import de.crysxd.octoapp.framework.waitFor
import de.crysxd.octoapp.framework.waitForDialog
import de.crysxd.octoapp.framework.waitTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import timber.log.Timber
import java.net.InetAddress

class ManualLoginTest {

    @get:Rule
    val activityRule = LazyActivityScenarioRule<MainActivity>(launchActivity = false) {
        Intent(InstrumentationRegistry.getInstrumentation().targetContext, MainActivity::class.java)
    }

    private val discoverUseCase = mock<DiscoverOctoPrintUseCase>()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val testEnv = TestEnvironmentLibrary.Terrier
    private val manualInput get() = onView(allOf(withId(R.id.input), isAssignableFrom(EditText::class.java))).check(matches(isDisplayed()))
    private val continueButton get() = onView(withText(R.string.sign_in___continue))

    @Before
    fun setUp() {
        val base = de.crysxd.octoapp.base.di.Injector.get()
        val mockBase = MockBaseComponent(base)
        de.crysxd.octoapp.base.di.Injector.set(mockBase)
        de.crysxd.octoapp.signin.di.Injector.init(mockBase)
    }

    @Test
    fun WHEN_no_instances_are_found_THEN_we_directly_move_to_manual_and_can_sign_in() = runBlocking {
        // GIVEN
        whenever(discoverUseCase.execute(Unit)).thenReturn(flowOf(DiscoverOctoPrintUseCase.Result(emptyList())))
        activityRule.launch()

        // Check loading
        onView(allOf(withId(R.id.title), withText(R.string.sign_in___discovery___welcome_title)))
            .check(matches(isDisplayed()))

//        manualInput.check(matches(isDisplayed()))

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

    @Test
    fun WHEN_some_instances_are_found_THEN_we_can_still_move_to_manual() = runBlocking {
        // GIVEN
        whenever(discoverUseCase.execute(Unit)).thenReturn(
            flowOf(
                DiscoverOctoPrintUseCase.Result(
                    listOf(
                        baseOption,
                        baseOption.copy(label = "Terrier"),
                        baseOption.copy(label = "Beagle"),
                        baseOption.copy(label = "Dachshund"),
                    )
                )
            )
        )
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

    @After
    fun tearDown() {
        reset(discoverUseCase)
    }

    private val baseOption = DiscoverOctoPrintUseCase.DiscoveredOctoPrint(
        label = "Frenchie",
        detailLabel = "Discovered via mock",
        host = InetAddress.getLocalHost(),
        quality = 100,
        method = DiscoverOctoPrintUseCase.DiscoveryMethod.DnsSd,
        port = -1,
        webUrl = "https://frenchie.com"
    )

    inner class MockBaseComponent(real: BaseComponent) : BaseComponent by real {
        override fun discoverOctoPrintUseCase(): DiscoverOctoPrintUseCase = discoverUseCase
        override fun requestApiAccessUseCase() = mock<RequestApiAccessUseCase>().also {
            runBlocking {
                try {
                    whenever(it.execute(any())).thenAnswer {
                        // Check correct web url
                        val params = it.arguments.mapNotNull { it as? RequestApiAccessUseCase.Params }.first()
                        assertThat(params.webUrl).isEqualTo(testEnv.webUrl)

                        // Return flow
                        flow {
                            repeat(5) {
                                emit(RequestApiAccessUseCase.State.Pending)
                                delay(200)
                            }
                            emit(RequestApiAccessUseCase.State.AccessGranted(apiKey = testEnv.apiKey))
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e)
                }
            }
        }
    }
}
