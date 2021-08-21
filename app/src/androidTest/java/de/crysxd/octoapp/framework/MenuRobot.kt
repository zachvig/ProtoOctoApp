package de.crysxd.octoapp.framework

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import de.crysxd.octoapp.R
import org.hamcrest.Matchers.allOf

object MenuRobot {

    fun openMenuWithMoreButton() {
        val matcher = allOf(withId(R.id.buttonMore), isDisplayed())
        waitFor(matcher)
        onView(matcher).perform(click())
        onView(withText(R.string.main_menu___item_show_printer)).check(matches(isDisplayed()))
        onView(withText(R.string.main_menu___item_show_settings)).check(matches(isDisplayed()))
        onView(withText(R.string.main_menu___item_show_octoprint)).check(matches(isDisplayed()))
        onView(withText(R.string.main_menu___item_news)).check(matches(isDisplayed()))
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
        waitTime(4000)
    }
}