package de.crysxd.octoapp.framework

import androidx.annotation.IdRes
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.swipeRight
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.adevinta.android.barista.rule.BaristaRule
import de.crysxd.octoapp.MainActivity
import de.crysxd.octoapp.R

object BottomToolbarRobot {

    fun confirmButtonWithSwipe(@IdRes buttonId: Int, barista: BaristaRule<MainActivity>) {
        val controlCenter = barista.activityTestRule.activity.controlCenter
        val wasEnabled = controlCenter.isEnabled
        controlCenter.isEnabled = false
        onView(withId(buttonId)).perform(click())
        waitTime(200)
        onView(withId(R.id.swipeButtons)).perform(swipeRight())

        controlCenter.isEnabled = wasEnabled
    }

    fun clickButton(@IdRes buttonId: Int) {
        onView(withId(buttonId)).perform(click())
    }
}