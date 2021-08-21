package de.crysxd.octoapp.files

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import de.crysxd.octoapp.R
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.framework.MenuRobot
import de.crysxd.octoapp.framework.TestEnvironmentLibrary
import de.crysxd.octoapp.framework.WorkspaceRobot
import de.crysxd.octoapp.framework.rules.IdleTestEnvironmentRule
import de.crysxd.octoapp.framework.rules.LazyMainActivityScenarioRule
import de.crysxd.octoapp.framework.waitFor
import de.crysxd.octoapp.framework.waitForDialog
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test

class StartPrintTest {

    private val testEnvVanilla = TestEnvironmentLibrary.Terrier
    private val testEnvSpoolManager = TestEnvironmentLibrary.Frenchie

    @get:Rule
    val activityRule = LazyMainActivityScenarioRule()

    @get:Rule
    val idleRule = IdleTestEnvironmentRule(testEnvSpoolManager, testEnvVanilla)

    @Test(timeout = 120_000)
    fun WHEN_a_print_is_started_THEN_the_app_shows_printing() {
        // GIVEN
        Injector.get().octorPrintRepository().setActive(testEnvVanilla)
        activityRule.launch()

        // Open file and start print
        triggerPrint()

        // Wait for print workspace to be shown
        verifyPrinting()

        // Pause and resume
        onView(withText(R.string.pause)).perform(click())
        waitForDialog(withText(R.string.pause_print_confirmation_message))
        onView(withText(R.string.pause_print_confirmation_action)).inRoot(isDialog()).perform(click())
        waitFor(allOf(withText(R.string.pausing), isDisplayed()))
        waitFor(allOf(withText(R.string.resume), isDisplayed()), timeout = 45_000)
        onView(withText(R.string.resume)).perform(click())
        waitForDialog(withText(R.string.resume_print_confirmation_message))
        onView(withText(R.string.resume_print_confirmation_action)).inRoot(isDialog()).perform(click())
        waitFor(allOf(withText(R.string.pause), isDisplayed()), timeout = 10_000)

        // Cancel print
        MenuRobot.openMenuWithMoreButton()
        MenuRobot.clickMenuButton(R.string.main_menu___item_cancel_print)
        waitForDialog(withText(R.string.cancel_print_confirmation_message))
        onView(withText(R.string.cancel_print_confirmation_action)).inRoot(isDialog()).perform(click())
        MenuRobot.waitForMenuToBeClosed()
        WorkspaceRobot.waitForPrepareWorkspace()
    }

    @Test(timeout = 60_000)
    fun WHEN_a_print_is_started_and_a_spool_is_selected_with_SpoolManager_THEN_the_app_shows_printing() =
        runMaterialTest("SM Spätzle")

    @Test(timeout = 60_000)
    fun WHEN_a_print_is_started_and_a_spool_is_selected_with_Filament_Manager_THEN_the_app_shows_printing() =
        runMaterialTest("FM Fusili (ABS)")

    @Test(timeout = 60_000)
    fun WHEN_a_print_is_started_and_no_spool_is_selected_THEN_the_app_shows_printing() =
        runMaterialTest(InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.material_menu___print_without_selection))

    private fun runMaterialTest(selection: String) {
        // GIVEN
        Injector.get().octorPrintRepository().setActive(testEnvSpoolManager)
        activityRule.launch()

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
        onView(withText("layers.gcode")).perform(click())
        onView(withText(R.string.start_printing)).perform(click())
    }

    private fun verifyPrinting() {
        // Wait for print workspace
        WorkspaceRobot.waitForPrintWorkspace()

        // Wait for print data to show up
        waitFor(allOf(withText(R.string.less_than_a_minute), isDisplayed()))
        onView(withText("layers.gcode")).check(matches(isDisplayed()))
    }

    private fun verifyMaterialSelection() {
        waitForDialog(withText(R.string.material_menu___title_select_material))
        onView(withText("FM Fusili (ABS)")).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(withText("FM Fusili (PLA)")).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(withText("SM Ramen (PLA, Japan, 2)")).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(withText("SM Ramen (PLA, Japan, 4)")).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(withText("SM Spaghetti")).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(withText("SM Spätzle")).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(withText("Print without selection")).inRoot(isDialog()).check(matches(isDisplayed()))
    }
}