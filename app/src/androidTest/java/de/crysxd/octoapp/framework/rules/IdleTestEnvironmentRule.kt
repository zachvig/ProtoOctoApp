package de.crysxd.octoapp.framework.rules

import android.app.Application
import androidx.test.platform.app.InstrumentationRegistry
import de.crysxd.octoapp.base.data.models.OctoPrintInstanceInformationV3
import de.crysxd.octoapp.base.di.BaseInjector
import de.crysxd.octoapp.framework.VirtualPrinterUtils.setVirtualPrinterEnabled
import de.crysxd.octoapp.octoprint.models.connection.ConnectionCommand
import kotlinx.coroutines.runBlocking
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import timber.log.Timber
import java.util.Date
import java.util.concurrent.TimeoutException

class IdleTestEnvironmentRule(private vararg val envs: OctoPrintInstanceInformationV3) : TestRule {

    override fun apply(base: Statement, description: Description) = object : Statement() {
        override fun evaluate() {
            try {
                // Disconnect all printer
                val start = System.currentTimeMillis()
                val end = System.currentTimeMillis() + 15_000
                runBlocking {
                    val octoprints = envs.map { BaseInjector.get().octoPrintProvider().createAdHocOctoPrint(it) }
                    octoprints.forEach {
                        Timber.i("Disconnecting printer from ${it.webUrl}")
                        it.createConnectionApi().executeConnectionCommand(ConnectionCommand.Disconnect)
                    }
                    octoprints.forEach {
                        var idle: Boolean
                        do {
                            Timber.i("Checking if ${it.webUrl} is idle...")
                            idle = it.createConnectionApi().getConnection().current.port == null
                        } while (!idle && System.currentTimeMillis() < end)
                        Timber.i("${it.webUrl} is idle")
                    }
                    envs.forEach {
                        Timber.i("Turning virtual printer on for ${it.webUrl}")
                        it.setVirtualPrinterEnabled(true)
                    }

                    if (System.currentTimeMillis() > end) {
                        throw TimeoutException("Unable to prepare environment before ${Date(end)}")
                    }
                }
                base.evaluate()
            } finally {
                BaseInjector.init(InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application)
                de.crysxd.octoapp.signin.di.SignInInjector.init(BaseInjector.get())
            }
        }
    }
}