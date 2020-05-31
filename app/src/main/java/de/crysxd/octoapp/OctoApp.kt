package de.crysxd.octoapp

import android.app.Application
import timber.log.Timber
import de.crysxd.octoapp.pre_print_controls.di.Injector as ConnectPrinterInjector
import de.crysxd.octoapp.signin.di.Injector as SignInInjector
import de.crysxd.octoapp.base.di.Injector as BaseInjector
import de.crysxd.octoapp.connect_printer.di.Injector as PrePrintControlsInjector

class OctoApp : Application() {

    override fun onCreate() {
        super.onCreate()

        Timber.plant(Timber.DebugTree())
        BaseInjector.init(this)
        SignInInjector.init(BaseInjector.get())
        ConnectPrinterInjector.init(BaseInjector.get())
        PrePrintControlsInjector.init(BaseInjector.get())
    }
}