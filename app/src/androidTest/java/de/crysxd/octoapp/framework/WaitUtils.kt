package de.crysxd.octoapp.framework

import android.view.View
import androidx.test.espresso.Espresso
import androidx.test.espresso.PerformException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.util.HumanReadables
import androidx.test.espresso.util.TreeIterables
import org.hamcrest.Matcher
import java.util.concurrent.TimeoutException

private fun waitForAction(
    matcher: Matcher<View>?,
    inverted: Boolean,
    timeout: Long = 3000L
): ViewAction {
    return object : ViewAction {
        override fun getConstraints(): Matcher<View> {
            return ViewMatchers.isRoot()
        }

        override fun getDescription(): String {
            return "wait for a specific view with $matcher during $timeout millis."
        }

        override fun perform(uiController: UiController, view: View) {
            uiController.loopMainThreadUntilIdle()
            val startTime = System.currentTimeMillis()
            val endTime = startTime + timeout
            do {
                val match = TreeIterables.breadthFirstViewTraversal(view).any {
                    matcher?.matches(it) ?: false
                }

                if (inverted != match) {
                    return
                }

                uiController.loopMainThreadForAtLeast(50)
            } while (System.currentTimeMillis() < endTime)

            if (matcher != null) {
                throw PerformException.Builder()
                    .withActionDescription(this.description)
                    .withViewDescription(HumanReadables.describe(view))
                    .withCause(TimeoutException())
                    .build()
            }
        }
    }
}

/**
 * Perform action of waiting for a specific view id.
 * @param viewId The id of the view to wait for.
 * @param timeout The timeout of until when to wait for.
 */
fun waitFor(
    viewMatcher: Matcher<View>,
    timeout: Long = 3000L
) {
    Espresso.onView(ViewMatchers.isRoot()).perform(waitForAction(viewMatcher, false, timeout))
}

/**
 * Perform action of waiting for a specific view id.
 * @param viewId The id of the view to wait for.
 * @param timeout The timeout of until when to wait for.
 */
fun waitForDialog(
    viewMatcher: Matcher<View>,
    timeout: Long = 3000L
) {
    Espresso.onView(ViewMatchers.isRoot()).inRoot(isDialog()).perform(waitForAction(viewMatcher, false, timeout))
}

fun waitTime(time: Long) {
    Espresso.onView(ViewMatchers.isRoot()).perform(waitForAction(null, false, time))
}

/**
 * Perform action of waiting for a specific view id.
 * @param viewId The id of the view to wait for.
 * @param timeout The timeout of until when to wait for.
 */
fun waitForNot(
    viewMatcher: Matcher<View>,
    timeout: Long = 3000L
) {
    Espresso.onView(ViewMatchers.isRoot()).perform(waitForAction(viewMatcher, true, timeout))
}
