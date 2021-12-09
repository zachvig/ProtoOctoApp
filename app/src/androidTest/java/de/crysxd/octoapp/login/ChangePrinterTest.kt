package de.crysxd.octoapp.login

import androidx.test.platform.app.InstrumentationRegistry
import com.adevinta.android.barista.rule.BaristaRule
import com.adevinta.android.barista.rule.flaky.AllowFlaky
import com.google.common.truth.Truth.assertThat
import de.crysxd.octoapp.MainActivity
import de.crysxd.octoapp.R
import de.crysxd.octoapp.base.billing.BillingManager
import de.crysxd.octoapp.base.di.BaseInjector
import de.crysxd.octoapp.framework.MenuRobot
import de.crysxd.octoapp.framework.SignInRobot
import de.crysxd.octoapp.framework.TestEnvironmentLibrary
import de.crysxd.octoapp.framework.WorkspaceRobot
import de.crysxd.octoapp.framework.rules.AutoConnectPrinterRule
import de.crysxd.octoapp.framework.rules.IdleTestEnvironmentRule
import de.crysxd.octoapp.framework.rules.MockDiscoveryRule
import de.crysxd.octoapp.framework.rules.ResetDaggerRule
import de.crysxd.octoapp.framework.rules.TestDocumentationRule
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

class ChangePrinterTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val testEnv1 = TestEnvironmentLibrary.Terrier
    private val testEnv2 = TestEnvironmentLibrary.Frenchie

    private val baristaRule = BaristaRule.create(MainActivity::class.java)
    private val discoveryRule = MockDiscoveryRule()

    @get:Rule
    val chain = RuleChain.outerRule(baristaRule)
        .around(IdleTestEnvironmentRule(testEnv1, testEnv2))
        .around(TestDocumentationRule())
        .around(ResetDaggerRule())
        .around(AutoConnectPrinterRule())
        .around(discoveryRule)

    @Before
    fun setUp() {
        val repo = BaseInjector.get().octorPrintRepository()
        repo.setActive(testEnv2)
        repo.setActive(testEnv1)
        discoveryRule.mockForRandomFound()
    }

    @After
    fun tearDown() {
        BillingManager.enabledForTest = null
    }

    @Test(timeout = 30_000)
    @AllowFlaky(attempts = 3)
    fun WHEN_quick_switch_is_enabled_THEN_OctoPrint_can_be_switched() {
        // GIVEN
        BillingManager.enabledForTest = true
        baristaRule.launchActivity()

        // Wait for ready
        WorkspaceRobot.waitForPrepareWorkspace()

        // Open menu and navigate
        MenuRobot.openMenuWithMoreButton()
        MenuRobot.clickMenuButton(R.string.main_menu___item_show_settings)
        MenuRobot.assertMenuTitle(R.string.main_menu___menu_settings_title)
        MenuRobot.clickMenuButton(R.string.main_menu___item_change_octoprint_instance)
        MenuRobot.assertMenuTitle(R.string.main_menu___title_quick_switch)
        MenuRobot.clickMenuButton(context.getString(R.string.main_menu___switch_to_octoprint, testEnv2.label))

        // Wait for switch completed
        WorkspaceRobot.waitForPrepareWorkspace()
        assertThat(BaseInjector.get().octorPrintRepository().getActiveInstanceSnapshot()?.webUrl).isEqualTo(testEnv2.webUrl)
    }

    @Test(timeout = 45_000)
    @AllowFlaky(attempts = 3)
    fun WHEN_quick_switch_is_disabled_THEN_OctoPrint_can_not_be_switched() {
        // GIVEN
        BillingManager.enabledForTest = false
        baristaRule.launchActivity()

        // Wait for ready
        WorkspaceRobot.waitForPrepareWorkspace()

        // Open menu and navigate
        MenuRobot.openMenuWithMoreButton()
        MenuRobot.clickMenuButton(R.string.main_menu___item_show_settings)
        MenuRobot.assertMenuTitle(R.string.main_menu___menu_settings_title)
        MenuRobot.clickMenuButton(R.string.main_menu___item_change_octoprint_instance)
        MenuRobot.assertMenuTitle(R.string.main_menu___title_quick_switch_disabled)
        MenuRobot.clickMenuButton(R.string.main_menu___item_sign_out)

        // Wait for sign out completed
        SignInRobot.waitForDiscoveryOptionsToBeShown()
        assertThat(BaseInjector.get().octorPrintRepository().getActiveInstanceSnapshot()?.webUrl).isNull()
    }
}