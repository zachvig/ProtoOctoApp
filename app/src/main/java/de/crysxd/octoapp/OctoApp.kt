package de.crysxd.octoapp

import android.app.Application
import timber.log.Timber
import de.crysxd.octoapp.base.di.Injector as BaseInjector
import de.crysxd.octoapp.connect_printer.di.Injector as ConnectPrintInjector
import de.crysxd.octoapp.pre_print_controls.di.Injector as PrePrintControlsInjector
import de.crysxd.octoapp.print_controls.di.Injector as PrintControlsInjector
import de.crysxd.octoapp.signin.di.Injector as SignInInjector

class OctoApp : Application() {

    override fun onCreate() {
        super.onCreate()

        Timber.plant(Timber.DebugTree())
        val wrapped = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            Timber.wtf(e)
            wrapped?.uncaughtException(t, e)
        }

        BaseInjector.init(this)
        SignInInjector.init(BaseInjector.get())
        ConnectPrintInjector.init(BaseInjector.get())
        PrePrintControlsInjector.init(BaseInjector.get())
        PrintControlsInjector.init(BaseInjector.get())
    }
}