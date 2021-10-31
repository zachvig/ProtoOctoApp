package de.crysxd.octoapp.framework

import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import de.crysxd.octoapp.R
import org.hamcrest.Matchers.allOf

object WorkspaceRobot {

    fun waitForConnectWorkspace() {
        waitFor(allOf(withId(R.id.textViewStep1Label), isDisplayed()), timeout = 10_000)
    }

    fun waitForPrepareWorkspace() {
        waitFor(allOf(withText(R.string.start_printing), isDisplayed()), timeout = 10_000)
    }

    fun waitForPrintWorkspace() {
        waitFor(allOf(withText(R.string.pause), isDisplayed()), timeout = 10_000)
    }
}