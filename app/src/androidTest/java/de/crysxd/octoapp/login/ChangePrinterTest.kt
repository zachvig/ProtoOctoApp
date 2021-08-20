package de.crysxd.octoapp.login

import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import de.crysxd.octoapp.MainActivity
import de.crysxd.octoapp.R
import de.crysxd.octoapp.base.billing.BillingManager
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.framework.MenuRobot
import de.crysxd.octoapp.framework.SignInRobot
import de.crysxd.octoapp.framework.TestEnvironmentLibrary
import de.crysxd.octoapp.framework.rules.LazyActivityScenarioRule
import de.crysxd.octoapp.framework.rules.MockDiscoveryRule
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ChangePrinterTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val testEnv1 = TestEnvironmentLibrary.Terrier
    private val testEnv2 = TestEnvironmentLibrary.Frenchie

    @get:Rule
    val activityRule = LazyActivityScenarioRule<MainActivity>(launchActivity = false) {
        Intent(InstrumentationRegistry.getInstrumentation().targetContext, MainActivity::class.java)
    }

    @get:Rule
    val discoveryRule = MockDiscoveryRule()


    @Before
    fun setUp() {
        val repo = Injector.get().octorPrintRepository()
        repo.setActive(testEnv2)
        repo.setActive(testEnv1)
        discoveryRule.mockForRandomFound()
    }

    @After
    fun tearDown() {
        BillingManager.enabledForTest = null
    }

    @Test(timeout = 30_000)
    fun WHEN_quick_switch_is_enabled_THEN_OctoPrint_can_be_switched() {
        // GIVEN
        BillingManager.enabledForTest = true
        activityRule.launch()

        // Wait for ready
        SignInRobot.waitForSignInToBeCompleted(skipAccess = true)

        // Open menu and navigate
        MenuRobot.openMenuWithMoreButton()
        MenuRobot.clickMenuButton(R.string.main_menu___item_show_settings)
        MenuRobot.assertMenuTitle(R.string.main_menu___menu_settings_title)
        MenuRobot.clickMenuButton(R.string.main_menu___item_change_octoprint_instance)
        MenuRobot.assertMenuTitle(R.string.main_menu___title_quick_switch)
        MenuRobot.clickMenuButton(context.getString(R.string.main_menu___switch_to_octoprint, testEnv2.label))

        // Wait for switch completed
        SignInRobot.waitForSignInToBeCompleted(skipAccess = true)
        assertThat(Injector.get().octorPrintRepository().getActiveInstanceSnapshot()?.webUrl).isEqualTo(testEnv2.webUrl)
    }

    @Test(timeout = 45_000)
    fun WHEN_quick_switch_is_disabled_THEN_OctoPrint_can_not_be_switched() {
        // GIVEN
        BillingManager.enabledForTest = false
        activityRule.launch()

        // Wait for ready
        SignInRobot.waitForSignInToBeCompleted(skipAccess = true)

        // Open menu and navigate
        MenuRobot.openMenuWithMoreButton()
        MenuRobot.clickMenuButton(R.string.main_menu___item_show_settings)
        MenuRobot.assertMenuTitle(R.string.main_menu___menu_settings_title)
        MenuRobot.clickMenuButton(R.string.main_menu___item_change_octoprint_instance)
        MenuRobot.assertMenuTitle(R.string.main_menu___title_quick_switch_disabled)
        MenuRobot.clickMenuButton(R.string.main_menu___item_sign_out)

        // Wait for sign out completed
        SignInRobot.waitForDiscoveryOptionsToBeShown()
        assertThat(Injector.get().octorPrintRepository().getActiveInstanceSnapshot()?.webUrl).isNull()
    }
}