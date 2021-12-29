package de.crysxd.octoapp.framework

import android.view.View
import androidx.test.espresso.AmbiguousViewMatcherException
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingRootException
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import de.crysxd.octoapp.R
import junit.framework.AssertionFailedError
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf

object MenuRobot {

    fun openMenuWithMoreButton() {
        val matcher = allOf(withId(R.id.menu), isDisplayed())
        waitFor(matcher)

        // Button is not 90% visible, use custom actions to circumvent check
        onView(matcher).perform(object : ViewAction {
            override fun getConstraints(): Matcher<View> = isDisplayed()
            override fun getDescription() = "Click button"
            override fun perform(uiController: UiController?, view: View) {
                view.performClick()
            }
        })
        onView(withText(R.string.main_menu___item_show_printer)).check(matches(isDisplayed()))
        onView(withText(R.string.main_menu___item_show_settings)).check(matches(isDisplayed()))
        onView(withText(R.string.main_menu___item_show_octoprint)).check(matches(isDisplayed()))
        onView(withText(R.string.main_menu___item_show_tutorials)).check(matches(isDisplayed()))


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