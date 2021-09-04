package de.crysxd.octoapp.framework.rules

import android.app.Application
import androidx.test.platform.app.InstrumentationRegistry
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.initializeDagger
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class ResetDaggerRule : TestWatcher() {

    override fun failed(e: Throwable?, description: Description?) {
        super.failed(e, description)
        resetDagger()
    }

    override fun succeeded(description: Description?) {
        super.succeeded(description)
        resetDagger()
    }

    private fun resetDagger() {
        Injector.init(InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application)
        initializeDagger()
    }
}