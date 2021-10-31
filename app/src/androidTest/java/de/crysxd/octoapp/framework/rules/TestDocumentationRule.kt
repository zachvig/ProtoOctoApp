package de.crysxd.octoapp.framework.rules

import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.screenshot.Screenshot
import de.crysxd.octoapp.BuildConfig
import de.crysxd.octoapp.base.di.BaseInjector
import de.crysxd.octoapp.base.ext.asStyleFileSize
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import timber.log.Timber
import java.io.File
import java.util.Locale

class TestDocumentationRule : TestWatcher() {

    private val id = String.format(Locale.ROOT, "%x", (0..1024).random())

    override fun starting(description: Description) {
        super.starting(description)
        BaseInjector.get().timberCacheTree().also {
            it.collectVerbose = true
            it.clear()
        }
        Timber.w("Starting ${description.testName}")
    }

    override fun failed(e: Throwable?, description: Description) {
        super.failed(e, description)
        val bitmap = Screenshot.capture().bitmap
        val screenshotFile = getFile(description, "webp")
        Timber.i("Writing screenshot to ${screenshotFile.absolutePath}")
        bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 80, screenshotFile.outputStream())
        val logsFile = getFile(description, "log")
        val logs = BaseInjector.get().timberCacheTree().logs
        Timber.i("Writing logs to ${logsFile.absolutePath} (${logs.length.toLong().asStyleFileSize()})")
        logsFile.writeText(logs)
    }

    private fun getFile(description: Description, suffix: String): File {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dir = File(BuildConfig.FAILED_TEST_SCREENSHOT_DIR)
        if (!dir.exists()) dir.mkdirs()
        val filename = "${description.testName}.$suffix"
        return File(dir, filename)
    }

    private val Description.testName get() = "${className.split(".").last()}___${methodName}___$id"
}