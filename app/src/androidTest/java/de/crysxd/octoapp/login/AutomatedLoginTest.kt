package de.crysxd.octoapp.login

import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import de.crysxd.octoapp.MainActivity
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.framework.SignInUtils
import de.crysxd.octoapp.framework.TestEnvironmentLibrary
import de.crysxd.octoapp.framework.rules.AcceptAllAccessRequestRule
import de.crysxd.octoapp.framework.rules.LazyActivityScenarioRule
import de.crysxd.octoapp.framework.rules.MockDiscoveryRule
import de.crysxd.octoapp.framework.rules.MockTestFullNetworkStackRule
import org.junit.Rule
import org.junit.Test

class AutomatedLoginTest {

    private val testEnv = TestEnvironmentLibrary.Terrier

    @get:Rule
    val activityRule = LazyActivityScenarioRule<MainActivity>(launchActivity = false) {
        Intent(InstrumentationRegistry.getInstrumentation().targetContext, MainActivity::class.java)
    }

    @get:Rule
    val discoveryRule = MockDiscoveryRule()

    @get:Rule
    val acceptAccessRequestRule = AcceptAllAccessRequestRule(testEnv)

    @get:Rule
    val mockTestFullNetworkStackRule = MockTestFullNetworkStackRule()

    @Test(timeout = 30_000L)
    fun WHEN_connecting_to_a_discovered_instance_THEN_we_can_sign_in() {
        discoveryRule.mockForTestEnvironment(testEnv)
        activityRule.launch()

        SignInUtils.waitForWelcomeTitleToBeShown()
        SignInUtils.waitForDiscoveryOptionsToBeShown()
        SignInUtils.selectDiscoveryOptionWithText(testEnv.label)
        SignInUtils.waitForSignInToBeCompleted()

        // Auto discover should continue without any checks
        verifyZeroInteractions(Injector.get().testFullNetworkStackUseCase())
    }
}