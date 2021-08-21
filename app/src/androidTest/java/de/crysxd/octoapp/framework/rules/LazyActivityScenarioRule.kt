package de.crysxd.octoapp.framework.rules

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.screenshot.Screenshot
import de.crysxd.octoapp.BuildConfig
import de.crysxd.octoapp.base.di.Injector
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import timber.log.Timber
import java.io.File
import java.util.Locale

open class LazyActivityScenarioRule<A : Activity>(private val launchActivity: Boolean, private val startActivityIntent: Intent) : TestWatcher() {

    private val id = String.format(Locale.ROOT, "%x", (0..1024).random())
    private var scenarioSupplier: () -> ActivityScenario<A> = { ActivityScenario.launch(startActivityIntent) }
    private var scenario: ActivityScenario<A>? = null

    override fun starting(description: Description) {
        super.starting(description)
        if (launchActivity) {
            launch()
        }

        Handler(Looper.getMainLooper()).post {
            Toast.makeText(InstrumentationRegistry.getInstrumentation().targetContext, description.testName, Toast.LENGTH_SHORT).show()
        }
    }

    override fun failed(e: Throwable?, description: Description) {
        super.failed(e, description)
        val bitmap = Screenshot.capture().bitmap
        val screenshotFile = getFile(description, "webp")
        Timber.i("Writing screenshot to ${screenshotFile.absolutePath}")
        bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 80, screenshotFile.outputStream())
        val logsFile = getFile(description, "log")
        Timber.i("Writing logs to ${logsFile.absolutePath}")
        logsFile.writeText(Injector.get().timberCacheTree().logs)

        scenario?.close()
    }

    override fun succeeded(description: Description) {
        super.succeeded(description)
        scenario?.close()
    }

    fun launch(newIntent: Intent? = null) {
        Injector.get().timberCacheTree().also {
            it.collectVerbose = true
            it.clear()
        }

        if ((scenario?.state ?: Lifecycle.State.INITIALIZED) >= Lifecycle.State.CREATED) throw IllegalStateException("Scenario has already been launched!")
        newIntent?.let { scenarioSupplier = { ActivityScenario.launch(it) } }
        scenario = scenarioSupplier()
    }

    fun getScenario(): ActivityScenario<A> = checkNotNull(scenario)

    private fun getFile(description: Description, suffix: String): File {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dir = File(BuildConfig.FAILED_TEST_SCREENSHOT_DIR)
        if (!dir.exists()) dir.mkdirs()
        val filename = "${description.testName}.$suffix"
        return File(dir, filename)
    }

    private val Description.testName get() = "${className.split(".").last()}___${methodName}___$id"
}