package de.crysxd.octoapp.framework

import androidx.test.espresso.AmbiguousViewMatcherException
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingRootException
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParentIndex
import androidx.test.espresso.matcher.ViewMatchers.withText
import de.crysxd.octoapp.R
import junit.framework.AssertionFailedError
import org.hamcrest.Matchers.allOf

object MenuRobot {

    fun openMenuWithMoreButton() {
        val matcher = allOf(withId(R.id.buttonMore), isDisplayed())
        waitFor(matcher)
        onView(matcher).perform(click())
        onView(withText(R.string.main_menu___item_show_printer)).check(matches(isDisplayed()))
        onView(withText(R.string.main_menu___item_show_settings)).check(matches(isDisplayed()))
        onView(withText(R.string.main_menu___item_show_octoprint)).check(matches(isDisplayed()))
        onView(withText(R.string.main_menu___item_show_tutorials)).check(matches(isDisplayed()))
    }

    fun checkHasMenuButton(text: String, index: Int, exists: Boolean = true) {
        onView(
            allOf(
                withText(text),
                isDescendantOfA(
                    allOf(
                        withId(R.id.menuItemRoot),
                        if (exists) withParentIndex(index) else withId(R.id.menuItemRoot)
                    )
                )
            )
        ).inRoot(isDialog()).check(if (exists) matches(isDisplayed()) else doesNotExist())
    }

    fun clickMenuButton(label: Int) {
        onView(withText(label)).inRoot(isDialog()).perform(click())
    }

    fun clickMenuButton(label: String) {
        onView(withText(label)).inRoot(isDialog()).perform(click())
    }

    fun assertMenuTitle(title: Int) {
        onView(withText(title)).inRoot(isDialog()).check(matches(isDisplayed()))
    }

    fun waitForMenuToBeClosed() {
        fun isMenuOpen() = try {
            onView(withId(R.id.menuContainer)).check(matches(isDisplayed()))
            true
        } catch (e: AmbiguousViewMatcherException) {
            true
        } catch (e: NoMatchingViewException) {
            false
        } catch (e: NoMatchingRootException) {
            false
        } catch (e: AssertionFailedError) {
            // Is animating out
            true
        }

        while (isMenuOpen()) {
            waitTime(500)
        }
    }
}