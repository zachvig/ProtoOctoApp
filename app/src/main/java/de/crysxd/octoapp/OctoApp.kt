package de.crysxd.octoapp

import android.app.Application
import timber.log.Timber
import de.crysxd.octoapp.connect_printer.di.Injector as ConnectPrinterInjector
import de.crysxd.octoapp.signin.di.Injector as SignInInjector

class OctoApp : Application() {

    override fun onCreate() {
        super.onCreate()

        Timber.plant(Timber.DebugTree())
        SignInInjector.init(this)
        ConnectPrinterInjector.init(this)
    }
}