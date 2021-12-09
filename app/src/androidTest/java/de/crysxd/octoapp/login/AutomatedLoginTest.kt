package de.crysxd.octoapp.login

import com.adevinta.android.barista.rule.BaristaRule
import com.adevinta.android.barista.rule.flaky.AllowFlaky
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import de.crysxd.octoapp.MainActivity
import de.crysxd.octoapp.base.di.BaseInjector
import de.crysxd.octoapp.framework.SignInRobot
import de.crysxd.octoapp.framework.TestEnvironmentLibrary
import de.crysxd.octoapp.framework.rules.AcceptAllAccessRequestRule
import de.crysxd.octoapp.framework.rules.AutoConnectPrinterRule
import de.crysxd.octoapp.framework.rules.IdleTestEnvironmentRule
import de.crysxd.octoapp.framework.rules.MockDiscoveryRule
import de.crysxd.octoapp.framework.rules.MockTestFullNetworkStackRule
import de.crysxd.octoapp.framework.rules.ResetDaggerRule
import de.crysxd.octoapp.framework.rules.TestDocumentationRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

class AutomatedLoginTest {

    private val testEnv = TestEnvironmentLibrary.Terrier
    private val baristaRule = BaristaRule.create(MainActivity::class.java)
    private val discoveryRule = MockDiscoveryRule()

    @get:Rule
    val chain = RuleChain.outerRule(baristaRule)
        .around(IdleTestEnvironmentRule(testEnv))
        .around(TestDocumentationRule())
        .around(ResetDaggerRule())
        .around(discoveryRule)
        .around(MockTestFullNetworkStackRule())
        .around(AutoConnectPrinterRule())
        .around(AcceptAllAccessRequestRule(testEnv))

    @Test(timeout = 30_000L)
    @AllowFlaky(attempts = 3)
    fun WHEN_connecting_to_a_discovered_instance_THEN_we_can_sign_in() {
        discoveryRule.mockForTestEnvironment(testEnv)
        baristaRule.launchActivity()

        SignInRobot.waitForWelcomeTitleToBeShown()
        SignInRobot.waitForDiscoveryOptionsToBeShown()
        SignInRobot.selectDiscoveryOptionWithText(testEnv.label)
        SignInRobot.waitForSignInToBeCompleted()

        // Auto discover should continue without any checks
        verifyZeroInteractions(BaseInjector.get().testFullNetworkStackUseCase())
    }
}