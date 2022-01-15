package de.crysxd.octoapp.files

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import com.adevinta.android.barista.rule.BaristaRule
import com.adevinta.android.barista.rule.flaky.AllowFlaky
import de.crysxd.octoapp.MainActivity
import de.crysxd.octoapp.R
import de.crysxd.octoapp.base.di.BaseInjector
import de.crysxd.octoapp.framework.BottomToolbarRobot
import de.crysxd.octoapp.framework.MenuRobot
import de.crysxd.octoapp.framework.TestEnvironmentLibrary
import de.crysxd.octoapp.framework.WorkspaceRobot
import de.crysxd.octoapp.framework.rules.AutoConnectPrinterRule
import de.crysxd.octoapp.framework.rules.IdleTestEnvironmentRule
import de.crysxd.octoapp.framework.rules.ResetDaggerRule
import de.crysxd.octoapp.framework.rules.TestDocumentationRule
import de.crysxd.octoapp.framework.waitFor
import de.crysxd.octoapp.framework.waitForDialog
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

class StartPrintTest {

    private val testEnvVanilla = TestEnvironmentLibrary.Terrier
    private val testEnvSpoolManager = TestEnvironmentLibrary.Frenchie
    private val baristaRule = BaristaRule.create(MainActivity::class.java)

    @get:Rule
    val chain = RuleChain.outerRule(baristaRule)
        .around(IdleTestEnvironmentRule(testEnvSpoolManager, testEnvVanilla))
        .around(TestDocumentationRule())
        .around(ResetDaggerRule())
        .around(AutoConnectPrinterRule())

    @Test(timeout = 120_000)
    @AllowFlaky(attempts = 1)
    fun WHEN_a_print_is_started_THEN_the_app_shows_printing() {
        // GIVEN
        BaseInjector.get().octorPrintRepository().setActive(testEnvVanilla)
        baristaRule.launchActivity()

        // Open file and start print
        triggerPrint()

        // Wait for print workspace to be shown
        verifyPrinting()

        // Pause
        waitFor(allOf(withId(R.id.status), withText("1 %"), isDisplayed()), timeout = 60_000)
        onView(withId(R.id.pause_print)).check(matches(isDisplayed()))
        onView(withId(R.id.cancel_print)).check(matches(isDisplayed()))
        onView(withId(R.id.resume_print)).check(matches(not(isDisplayed())))
        BottomToolbarRobot.confirmButtonWithSwipe(R.id.pause_print, baristaRule)

        // Wait for paused and resume
        waitFor(allOf(withText(R.string.pausing), isDisplayed()))
        waitFor(allOf(withText(R.string.paused), isDisplayed()), timeout = 45_000)
        onView(withId(R.id.pause_print)).check(matches(not(isDisplayed())))
        onView(withId(R.id.cancel_print)).check(matches(isDisplayed()))
        onView(withId(R.id.resume_print)).check(matches(isDisplayed()))
        BottomToolbarRobot.confirmButtonWithSwipe(R.id.resume_print, baristaRule)

        // Wait for resume and cancel
        waitFor(allOf(withId(R.id.status), withText("2 %"), isDisplayed()), timeout = 45_000)
        onView(withId(R.id.pause_print)).check(matches(isDisplayed()))
        onView(withId(R.id.cancel_print)).check(matches(isDisplayed()))
        onView(withId(R.id.resume_print)).check(matches(not(isDisplayed())))
        BottomToolbarRobot.confirmButtonWithSwipe(R.id.cancel_print, baristaRule)
        WorkspaceRobot.waitForPrepareWorkspace()
    }

    @Test(timeout = 60_000)
    @AllowFlaky(attempts = 3)
    fun WHEN_a_print_is_started_and_a_spool_is_selected_with_SpoolManager_THEN_the_app_shows_printing() =
        runMaterialTest("SM Spätzle")

    @Test(timeout = 60_000)
    @AllowFlaky(attempts = 3)
    fun WHEN_a_print_is_started_and_a_spool_is_selected_with_Filament_Manager_THEN_the_app_shows_printing() =
        runMaterialTest("FM Fusili (ABS)")

    @Test(timeout = 60_000)
    @AllowFlaky(attempts = 3)
    fun WHEN_a_print_is_started_and_no_spool_is_selected_THEN_the_app_shows_printing() =
        runMaterialTest(InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.material_menu___print_without_selection))

    private fun runMaterialTest(selection: String) {
        // GIVEN
        BaseInjector.get().octorPrintRepository().setActive(testEnvSpoolManager)
        baristaRule.launchActivity()

        // Open file and start print
        triggerPrint()

        // Check dialog
        verifyMaterialSelection()
        onView(withText(selection)).inRoot(isDialog()).perform(click())

        // Wait for print workspace to be shown
        MenuRobot.waitForMenuToBeClosed()
        verifyPrinting()
    }

    private fun triggerPrint() {
        WorkspaceRobot.waitForPrepareWorkspace()
        onView(withText(R.string.start_printing)).perform(click())
        waitFor(allOf(withText("layers.gcode"), isDisplayed()))
        onView(withText("layers.gcode")).perform(click())
        waitFor(allOf(withText(R.string.start_printing), isDisplayed()))
        onView(withText(R.string.start_printing)).perform(click())
    }

    private fun verifyPrinting() {
        // Wait for print workspace
        WorkspaceRobot.waitForPrintWorkspace()

        // Wait for print data to show up
        waitFor(allOf(withText(R.string.less_than_a_minute), isDisplayed()), timeout = 10_000)
        waitFor(allOf(withText("layers.gcode"), isDisplayed()), timeout = 10_000)
    }

    private fun verifyMaterialSelection() {
        waitForDialog(withText(R.string.material_menu___title_select_material))
        onView(withText("FM Fusili (ABS)")).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(withText("FM Fusili (PLA)")).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(withText("SM Ramen (PLA, Japan, 2)")).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(withText("SM Ramen (PLA, Japan, 4)")).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(withText("SM Spaghetti")).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(withText("SM Spätzle")).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(withText("Template")).inRoot(isDialog()).check(doesNotExist())
        onView(withText("Not Active")).inRoot(isDialog()).check(doesNotExist())
        onView(withText("Empty")).inRoot(isDialog()).check(doesNotExist())
        onView(withText(R.string.material_menu___print_without_selection)).inRoot(isDialog()).check(matches(isDisplayed()))
    }
}