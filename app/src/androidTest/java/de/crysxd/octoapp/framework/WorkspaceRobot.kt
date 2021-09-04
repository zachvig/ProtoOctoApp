package de.crysxd.octoapp.framework

import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import de.crysxd.octoapp.R
import org.hamcrest.Matchers.allOf

object WorkspaceRobot {

    fun waitForConnectWorkspace() {
        waitFor(allOf(withId(R.id.textViewStep1Label), isDisplayed()), timeout = 10_000)
    }

    fun waitForPrepareWorkspace() {
        waitFor(allOf(withId(R.id.textViewStep2Label), isDisplayed()), timeout = 10_000)
    }

    fun waitForPrintWorkspace() {
        waitFor(allOf(withId(R.id.textViewStep3Label), isDisplayed()), timeout = 10_000)
    }
}