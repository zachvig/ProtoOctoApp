package de.crysxd.octoapp.framework.rules

import de.crysxd.octoapp.base.di.BaseInjector
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class AutoConnectPrinterRule : TestWatcher() {

    override fun starting(description: Description) {
        BaseInjector.get().octoPreferences().isAutoConnectPrinter = true
        BaseInjector.get().octoPreferences().wasAutoConnectPrinterInfoShown = true
    }
}