package de.crysxd.octoapp.login

import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.framework.SignInRobot
import de.crysxd.octoapp.framework.TestEnvironmentLibrary
import de.crysxd.octoapp.framework.rules.AcceptAllAccessRequestRule
import de.crysxd.octoapp.framework.rules.LazyMainActivityScenarioRule
import de.crysxd.octoapp.framework.rules.MockDiscoveryRule
import de.crysxd.octoapp.framework.rules.MockTestFullNetworkStackRule
import org.junit.Rule
import org.junit.Test

class AutomatedLoginTest {

    private val testEnv = TestEnvironmentLibrary.Terrier

    @get:Rule
    val activityRule = LazyMainActivityScenarioRule()

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

        SignInRobot.waitForWelcomeTitleToBeShown()
        SignInRobot.waitForDiscoveryOptionsToBeShown()
        SignInRobot.selectDiscoveryOptionWithText(testEnv.label)
        SignInRobot.waitForSignInToBeCompleted()

        // Auto discover should continue without any checks
        verifyZeroInteractions(Injector.get().testFullNetworkStackUseCase())
    }
}