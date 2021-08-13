package de.crysxd.octoapp.login

import android.content.Intent
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.platform.app.InstrumentationRegistry
import de.crysxd.octoapp.MainActivity
import de.crysxd.octoapp.R
import de.crysxd.octoapp.base.di.BaseComponent
import de.crysxd.octoapp.base.usecase.DiscoverOctoPrintUseCase
import de.crysxd.octoapp.framework.LazyActivityScenarioRule
import de.crysxd.octoapp.framework.waitFor
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.*
import java.net.InetAddress

class ManualLoginTest {

    @get:Rule
    val activityRule = LazyActivityScenarioRule<MainActivity>(launchActivity = false) {
        Intent(InstrumentationRegistry.getInstrumentation().targetContext, MainActivity::class.java)
    }

    private val discoverUseCase = mock(DiscoverOctoPrintUseCase::class.java)

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
        `when`(discoverUseCase.execute(Unit)).thenReturn(flowOf(DiscoverOctoPrintUseCase.Result(emptyList())))
        activityRule.launch()

        // Check loading
        onView(withId(R.id.title))
            .check(matches(isDisplayed()))
            .check(matches(withText(R.string.sign_in___discovery___welcome_title)))

        // Wait for loading done and move to manual
        waitFor(
            viewMatcher = allOf(
                withId(R.id.title),
                isDisplayed(),
                withText(R.string.sign_in___discovery___connect_manually_title)
            ),
            timeout = 5_000
        )
    }

    @Test
    fun WHEN_some_instances_are_found_THEN_we_can_still_move_to_manual() = runBlocking {
        // GIVEN
        `when`(discoverUseCase.execute(Unit)).thenReturn(
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
        waitFor(
            viewMatcher = allOf(
                withId(R.id.title),
                isDisplayed(),
                withText(R.string.sign_in___discovery___connect_manually_title)
            )
        )
    }

    @After
    fun tearDown() {
        activityRule.getScenario().close()
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
    }
}
