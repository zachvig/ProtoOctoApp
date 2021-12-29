package de.crysxd.octoapp.framework

import androidx.annotation.IdRes
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.swipeRight
import androidx.test.espresso.matcher.ViewMatchers.withId
import de.crysxd.octoapp.R

object BottomToolbarRobot {

    fun confirmButtonWithSwipe(@IdRes buttonId: Int) {
        onView(withId(buttonId)).perform(click())
        waitTime(200)
        onView(withId(R.id.swipeButtons)).perform(swipeRight())
    }

    fun clickButton(@IdRes buttonId: Int) {
        onView(withId(buttonId)).perform(click())
    }
}