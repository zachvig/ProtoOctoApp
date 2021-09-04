package de.crysxd.octoapp.files

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.adevinta.android.barista.rule.BaristaRule
import de.crysxd.octoapp.MainActivity
import de.crysxd.octoapp.R
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.framework.TestEnvironmentLibrary
import de.crysxd.octoapp.framework.WorkspaceRobot
import de.crysxd.octoapp.framework.rules.IdleTestEnvironmentRule
import de.crysxd.octoapp.framework.rules.ResetDaggerRule
import de.crysxd.octoapp.framework.rules.TestDocumentationRule
import de.crysxd.octoapp.framework.waitForNot
import org.hamcrest.Matchers.allOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

class ShowFilesTest {

    private val testEnv = TestEnvironmentLibrary.Terrier
    private val baristaRule = BaristaRule.create(MainActivity::class.java)

    @get:Rule
    val chain = RuleChain.outerRule(baristaRule)
        .around(IdleTestEnvironmentRule(testEnv))
        .around(TestDocumentationRule())
        .around(ResetDaggerRule())

    @Before
    fun setUp() {
        Injector.get().octorPrintRepository().setActive(testEnv)
    }

    @Test(timeout = 60_000)
    fun WHEN_files_are_opened_THEN_files_are_listed() {
        // GIVEN
        baristaRule.launchActivity()

        // Open files
        WorkspaceRobot.waitForPrepareWorkspace()
        onView(withText(R.string.start_printing)).perform(click())

        // Check slicer hint is shown and can be dismissed
        onView(withText(R.string.using_cura_or_prusa_slicer_install_a_plugin_to_get_thumbnail_previews_title)).check(matches(isDisplayed()))
        onView(withText(R.string.using_cura_or_prusa_slicer_install_a_plugin_to_get_thumbnail_previews_detail)).check(matches(isDisplayed()))
        onView(withText(R.string.hide)).perform(click())
        waitForNot(allOf(withText(R.string.hide), isDisplayed()))

        // Check files listed
        onView(withText(R.string.select_file_to_print)).check(matches(isDisplayed()))
        onView(withText("layers.gcode")).check(matches(isDisplayed()))
        onView(withText("CE3_886e3c50-0100-4d4f-bbbe-4508835bab2b.gcode")).check(matches(isDisplayed()))
        onView(withText("CE3_e6bea3cb-37c8-4a1c-8a4e-dd6cded40fa0.gcode")).check(matches(isDisplayed()))
        onView(withText("layers.gcode")).perform(click())

        // Check tabs
        onView(withText(R.string.file_details_tab_preview)).check(matches(isDisplayed()))
        onView(withText(R.string.file_details_tab_info)).check(matches(isDisplayed()))

        // Check some details
        onView(withText("layers.gcode")).check(matches(isDisplayed()))
        onView(withText(R.string.print_time)).check(matches(isDisplayed()))
        onView(withText(R.string.model_size)).check(matches(isDisplayed()))
        onView(withText(R.string.filament_use)).check(matches(isDisplayed()))
        onView(withText(R.string.location)).check(matches(isDisplayed()))
        onView(withText(R.string.path)).check(matches(isDisplayed()))
        onView(withText(R.string.size)).check(matches(isDisplayed()))
        onView(withText(R.string.uploaded)).check(matches(isDisplayed()))
        onView(withText(R.string.last_print)).check(matches(isDisplayed()))
    }
}