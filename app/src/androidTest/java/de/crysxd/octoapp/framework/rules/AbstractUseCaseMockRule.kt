package de.crysxd.octoapp.framework.rules

import android.app.Application
import androidx.test.platform.app.InstrumentationRegistry
import de.crysxd.octoapp.base.di.BaseComponent
import de.crysxd.octoapp.base.di.Injector
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

abstract class AbstractUseCaseMockRule : TestRule {

    protected abstract fun createBaseComponent(base: BaseComponent): BaseComponent

    override fun apply(base: Statement, description: Description) = object : Statement() {
        override fun evaluate() {
            try {
                val b = Injector.get()
                val mockBase = createBaseComponent(b)
                Injector.set(mockBase)
                de.crysxd.octoapp.signin.di.Injector.init(mockBase)
                base.evaluate()
            } finally {
                Injector.init(InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application)
                de.crysxd.octoapp.signin.di.Injector.init(Injector.get())
            }
        }
    }
}