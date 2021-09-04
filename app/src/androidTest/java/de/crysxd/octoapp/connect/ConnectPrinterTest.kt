package de.crysxd.octoapp.connect

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import com.adevinta.android.barista.rule.BaristaRule
import de.crysxd.octoapp.MainActivity
import de.crysxd.octoapp.R
import de.crysxd.octoapp.base.billing.BillingManager
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformationV3
import de.crysxd.octoapp.framework.MenuRobot
import de.crysxd.octoapp.framework.SignInRobot
import de.crysxd.octoapp.framework.TestEnvironmentLibrary
import de.crysxd.octoapp.framework.VirtualPrinterUtils.setVirtualPrinterEnabled
import de.crysxd.octoapp.framework.WorkspaceRobot
import de.crysxd.octoapp.framework.rules.AcceptAllAccessRequestRule
import de.crysxd.octoapp.framework.rules.IdleTestEnvironmentRule
import de.crysxd.octoapp.framework.rules.ResetDaggerRule
import de.crysxd.octoapp.framework.rules.TestDocumentationRule
import de.crysxd.octoapp.framework.waitFor
import de.crysxd.octoapp.framework.waitForDialog
import de.crysxd.octoapp.framework.waitTime
import de.crysxd.octoapp.octoprint.plugins.power.psucontrol.PsuControlPowerPlugin
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

class ConnectPrinterTest {

    private val testEnv = TestEnvironmentLibrary.Terrier
    private val powerControlsTestEnv = TestEnvironmentLibrary.Dachshund
    private val wrongEnv = OctoPrintInstanceInformationV3(
        id = "random",
        webUrl = "http://127.0.0.1:100".toHttpUrl(),
        apiKey = "XXXXXXX"
    )

    private val baristaRule = BaristaRule.create(MainActivity::class.java)

    @get:Rule
    val chain = RuleChain.outerRule(baristaRule)
        .around(IdleTestEnvironmentRule(testEnv, powerControlsTestEnv))
        .around(TestDocumentationRule())
        .around(ResetDaggerRule())
        .around(AcceptAllAccessRequestRule(testEnv))

    @Test(timeout = 30_000)
    fun WHEN_auto_connect_is_disabled_THEN_connect_button_can_be_used() {
        // GIVEN
        Injector.get().octorPrintRepository().setActive(testEnv)
        Injector.get().octoPreferences().isAutoConnectPrinter = false
        baristaRule.launchActivity()

        // Wait for ready to connect
        WorkspaceRobot.waitForConnectWorkspace()
        waitFor(allOf(withText(R.string.connect_printer___waiting_for_user_title)))
        waitTime(2000) // Wait to see if we auto connect
        onView(withText(R.string.connect_printer___begin_connection)).perform(click())
        waitForDialog(withText(R.string.connect_printer___begin_connection_cofirmation_positive))
        onView(withText(R.string.connect_printer___begin_connection_cofirmation_positive)).inRoot(isDialog()).perform(click())
        WorkspaceRobot.waitForPrepareWorkspace()

    }

    @Test(timeout = 30_000)
    fun WHEN_OctoPrint_not_available_and_no_quick_switch_THEN_other_OctoPrint_can_be_connected() {
        // GIVEN
        Injector.get().octorPrintRepository().setActive(wrongEnv)
        BillingManager.enabledForTest = false
        baristaRule.launchActivity()

        // Wait for ready to connect
        WorkspaceRobot.waitForConnectWorkspace()
        waitFor(allOf(withText(R.string.connect_printer___octoprint_not_available_title), isDisplayed()))
        onView(withText(R.string.connect_printer___action_change_octoprint)).perform(click())
        MenuRobot.assertMenuTitle(R.string.main_menu___title_quick_switch_disabled)
        MenuRobot.clickMenuButton(R.string.main_menu___item_sign_out)
        MenuRobot.waitForMenuToBeClosed()
        SignInRobot.waitForDiscoveryOptionsToBeShown()
    }

    @Test(timeout = 30_000)
    fun WHEN_OctoPrint_not_available_and_quick_switch_available_THEN_other_OctoPrint_can_be_connected() {
        // GIVEN
        Injector.get().octorPrintRepository().setActive(testEnv)
        Injector.get().octorPrintRepository().setActive(wrongEnv)
        BillingManager.enabledForTest = true
        baristaRule.launchActivity()

        // Wait for ready to connect
        WorkspaceRobot.waitForConnectWorkspace()
        waitFor(allOf(withText(R.string.connect_printer___octoprint_not_available_title), isDisplayed()))
        onView(withText(R.string.connect_printer___action_change_octoprint)).perform(click())
        MenuRobot.assertMenuTitle(R.string.main_menu___title_quick_switch)
        MenuRobot.clickMenuButton(InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.main_menu___switch_to_octoprint, testEnv.label))
        MenuRobot.waitForMenuToBeClosed()
        WorkspaceRobot.waitForPrepareWorkspace()
    }

    @Test(timeout = 30_000)
    fun WHEN_power_controls_are_available_THEN_psu_can_be_turned_on() = runBlocking {
        // GIVEN
        // We need a bit of wait before/after changing virtual printer, OctoPrint otherwise gets overloaded...
        // Also make sure PSU is turned off
        Injector.get().octorPrintRepository().setActive(powerControlsTestEnv)
        powerControlsTestEnv.setVirtualPrinterEnabled(false)
        val octoPrint = Injector.get().octoPrintProvider().createAdHocOctoPrint(powerControlsTestEnv)
        val settings = octoPrint.createSettingsApi().getSettings()
        octoPrint.createPowerPluginsCollection().plugins.first { it is PsuControlPowerPlugin }
            .getDevices(settings).first().turnOff()
        baristaRule.launchActivity()

        // Wait for ready and turn on PSU
        WorkspaceRobot.waitForConnectWorkspace()

        // Turn on printer (simulate by turning on virtual printer)
        waitFor(allOf(withText(R.string.connect_printer___action_turn_psu_on), isDisplayed()))
        onView(withText(R.string.connect_printer___action_turn_psu_on)).perform(click())
        MenuRobot.waitForMenuToBeClosed()
        waitFor(allOf(withText(R.string.connect_printer___action_turn_psu_off), isDisplayed()), timeout = 8000)
        powerControlsTestEnv.setVirtualPrinterEnabled(true)
        WorkspaceRobot.waitForPrepareWorkspace()
    }
}