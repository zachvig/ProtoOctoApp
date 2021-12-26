package de.crysxd.octoapp.webcam

import android.graphics.Point
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingPolicies
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.platform.app.InstrumentationRegistry
import com.adevinta.android.barista.rule.BaristaRule
import com.adevinta.android.barista.rule.flaky.AllowFlaky
import com.google.common.truth.Truth.assertThat
import de.crysxd.baseui.widget.webcam.WebcamView
import de.crysxd.octoapp.MainActivity
import de.crysxd.octoapp.R
import de.crysxd.octoapp.base.billing.BillingManager
import de.crysxd.octoapp.base.data.models.WidgetPreferences
import de.crysxd.octoapp.base.data.models.WidgetType
import de.crysxd.octoapp.base.di.BaseInjector
import de.crysxd.octoapp.base.network.MjpegConnection2
import de.crysxd.octoapp.framework.TestEnvironmentLibrary
import de.crysxd.octoapp.framework.rules.IdleTestEnvironmentRule
import de.crysxd.octoapp.framework.rules.ResetDaggerRule
import de.crysxd.octoapp.framework.rules.TestDocumentationRule
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import timber.log.Timber
import java.util.concurrent.TimeUnit

class WebcamTest {

    private val testEnv = TestEnvironmentLibrary.Terrier
    private val baristaRule = BaristaRule.create(MainActivity::class.java)

    @get:Rule
    val chain = RuleChain.outerRule(baristaRule)
        .around(IdleTestEnvironmentRule(testEnv))
        .around(TestDocumentationRule())
        .around(ResetDaggerRule())

    @Test(timeout = 60_000)
    @AllowFlaky(attempts = 2)
    fun WHEN_a_webcam_connection_is_made_THEN_frames_are_loaded() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().context
        val server = MjpegTestServer()
        BillingManager.enabledForTest = true
        val serverJob = server.start(context)
        val frameTimes = mutableListOf<Long>()
        var lastFrame: Long? = null
        var start = System.currentTimeMillis()
        val expectedFrames = 50
        val lastFrameDimensions = Point()
        try {
            val connection = MjpegConnection2("http://127.0.0.1:${server.port}".toHttpUrl(), "test", throwExceptions = true)
            connection.load().takeWhile {
                // We start 10 frames short to let the connection end gracefully on our (receivers) terms
                frameTimes.size < expectedFrames
            }.collect { state ->
                when (state) {
                    MjpegConnection2.MjpegSnapshot.Loading -> start = System.currentTimeMillis()
                    is MjpegConnection2.MjpegSnapshot.Frame -> {
                        lastFrame?.let {
                            frameTimes.add(System.currentTimeMillis() - it)
                        }
                        lastFrameDimensions.x = state.frame.width
                        lastFrameDimensions.y = state.frame.height
                        lastFrame = System.currentTimeMillis()
                    }
                }
            }
        } finally {
            BillingManager.enabledForTest = null
            serverJob.cancel()
            server.stop()

            val end = System.currentTimeMillis()
            val fps = expectedFrames / ((end - start) / 1000f)
            Timber.i("Webcam stats: avg=${frameTimes.average()}ms, total=${end - start}ms, rate=${fps}fps")

            assertThat(frameTimes.size).isEqualTo(expectedFrames)
            assertThat(fps).isAtLeast(30)
            assertThat(lastFrameDimensions.x).isEqualTo(1920)
            assertThat(lastFrameDimensions.y).isEqualTo(1080)
        }
    }

    @Test(timeout = 120_000)
    @AllowFlaky(attempts = 1)
    fun WHEN_a_webcam_connection_is_made_THEN_frames_are_shown() = runBlocking {
        val server = MjpegTestServer()
        val context = InstrumentationRegistry.getInstrumentation().context
        val serverJob = server.start(context)
        BaseInjector.get().octorPrintRepository().setActive(testEnv)
        BaseInjector.get().octoPreferences().isAutoConnectPrinter = true
        BaseInjector.get().octoPreferences().wasAutoConnectPrinterInfoShown = true
        BillingManager.enabledForTest = true

        // Put webcam on top
        BaseInjector.get().widgetPreferencesRepository().setWidgetOrder(
            "preprint",
            WidgetPreferences(
                "preprint",
                hidden = emptyList(),
                items = listOf(WidgetType.WebcamWidget)
            )
        )

        val idle = WebcamIdleResource()
        IdlingRegistry.getInstance().register(idle)
        IdlingPolicies.setIdlingResourceTimeout(300, TimeUnit.SECONDS)
        IdlingPolicies.setMasterPolicyTimeout(300, TimeUnit.SECONDS)

        var fps = 0f
        try {
            baristaRule.launchActivity()
            var start: Long = 0

            // Setup measurement
            WebcamView.frame1Callback = {
                start = System.currentTimeMillis()
            }
            WebcamView.frame1000Callback = {
                val end = System.currentTimeMillis()
                assertThat(start).isNotEqualTo(0)
                fps = 1_000 / ((end - start) / 1000f)
                Timber.i("Webcam stats: total=${end - start}ms, rate=${fps}fps")
                idle.testCompleted = true
            }

            // This is only to wait until the idle resource is idle (aka enough frames received)
            onView(isAssignableFrom(WebcamView::class.java)).check(matches(withId(R.id.webcamView)))
            assertThat(fps).isAtLeast(20)
        } finally {
            BillingManager.enabledForTest = null

            IdlingRegistry.getInstance().unregister(idle)
            IdlingPolicies.setIdlingResourceTimeout(30, TimeUnit.SECONDS)
            IdlingPolicies.setMasterPolicyTimeout(60, TimeUnit.SECONDS)
            serverJob.cancel()
            server.stop()
        }
    }

    private class WebcamIdleResource : IdlingResource {
        var testCompleted = false
            set(value) {
                field = value
                if (value) {
                    callbacks.forEach { it.onTransitionToIdle() }
                }
            }
        val callbacks = mutableListOf<IdlingResource.ResourceCallback>()

        override fun getName() = "Wait for webcam test"

        override fun isIdleNow() = testCompleted

        override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback) {
            callbacks.add(callback)
        }
    }
}