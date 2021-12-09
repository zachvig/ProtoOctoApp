package de.crysxd.octoapp.connect

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.adevinta.android.barista.rule.BaristaRule
import com.adevinta.android.barista.rule.flaky.AllowFlaky
import de.crysxd.octoapp.MainActivity
import de.crysxd.octoapp.R
import de.crysxd.octoapp.base.di.BaseInjector
import de.crysxd.octoapp.framework.MenuRobot.assertMenuTitle
import de.crysxd.octoapp.framework.MenuRobot.clickMenuButton
import de.crysxd.octoapp.framework.MenuRobot.waitForMenuToBeClosed
import de.crysxd.octoapp.framework.TestEnvironmentLibrary
import de.crysxd.octoapp.framework.WorkspaceRobot.waitForConnectWorkspace
import de.crysxd.octoapp.framework.WorkspaceRobot.waitForPrepareWorkspace
import de.crysxd.octoapp.framework.rules.AcceptAllAccessRequestRule
import de.crysxd.octoapp.framework.rules.IdleTestEnvironmentRule
import de.crysxd.octoapp.framework.rules.ResetDaggerRule
import de.crysxd.octoapp.framework.rules.TestDocumentationRule
import de.crysxd.octoapp.framework.waitFor
import de.crysxd.octoapp.framework.waitForDialog
import de.crysxd.octoapp.framework.waitTime
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

class AutoConnectSetupTest {

    private val testEnv = TestEnvironmentLibrary.Terrier
    private val baristaRule = BaristaRule.create(MainActivity::class.java)

    @get:Rule
    val chain = RuleChain.outerRule(baristaRule)
        .around(IdleTestEnvironmentRule(testEnv))
        .around(TestDocumentationRule())
        .around(ResetDaggerRule())
        .around(AcceptAllAccessRequestRule(testEnv))

    @Test(timeout = 30_000)
    @AllowFlaky(attempts = 3)
    fun WHEN_connect_is_opened_for_the_first_time_THEN_user_can_opt_for_manual_connect() {
        // GIVEN
        BaseInjector.get().octorPrintRepository().setActive(testEnv)
        baristaRule.launchActivity()

        // Wait for ready to connect
        waitForConnectWorkspace()
        waitFor(allOf(withText(R.string.connect_printer___waiting_for_user_title)))
        waitTime(2000) // Wait to see if we auto connect
        onView(withText(R.string.connect_printer___begin_connection)).perform(click())

        // Assert setup menu
        assertMenuTitle(R.string.connect_printer___auto_menu___title)
        clickMenuButton(R.string.connect_printer___auto_menu___manual_option)
        waitForMenuToBeClosed()

        // Manual connect flow triggered
        waitForDialog(withText(R.string.connect_printer___begin_connection_cofirmation_positive))
        onView(withText(R.string.connect_printer___begin_connection_cofirmation_positive)).inRoot(isDialog()).perform(click())
        waitForPrepareWorkspace()
    }

    @Test(timeout = 30_000)
    @AllowFlaky(attempts = 3)
    fun WHEN_connect_is_opened_for_the_first_time_THEN_user_can_opt_for_auto_connect() {
        // GIVEN
        BaseInjector.get().octorPrintRepository().setActive(testEnv)
        baristaRule.launchActivity()

        // Wait for ready to connect
        waitForConnectWorkspace()
        waitFor(allOf(withText(R.string.connect_printer___waiting_for_user_title)))
        waitTime(2000) // Wait to see if we auto connect
        onView(withText(R.string.connect_printer___begin_connection)).perform(click())

        // Assert setup menu
        assertMenuTitle(R.string.connect_printer___auto_menu___title)
        clickMenuButton(R.string.connect_printer___auto_menu___auto_option)
        waitForMenuToBeClosed()

        // Aut connect flow triggered
        waitForPrepareWorkspace()
    }
}
