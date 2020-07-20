package de.crysxd.octoapp

import android.app.Application
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import timber.log.Timber
import de.crysxd.octoapp.base.di.Injector as BaseInjector
import de.crysxd.octoapp.connect_printer.di.Injector as ConnectPrintInjector
import de.crysxd.octoapp.pre_print_controls.di.Injector as PrePrintControlsInjector
import de.crysxd.octoapp.print_controls.di.Injector as PrintControlsInjector
import de.crysxd.octoapp.signin.di.Injector as SignInInjector

class OctoApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Setup logging
        Timber.plant(Timber.DebugTree())
        val wrapped = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            Timber.wtf(e)
            wrapped?.uncaughtException(t, e)
        }

        // Setup Dagger
        BaseInjector.init(this)
        SignInInjector.init(BaseInjector.get())
        ConnectPrintInjector.init(BaseInjector.get())
        PrePrintControlsInjector.init(BaseInjector.get())
        PrintControlsInjector.init(BaseInjector.get())

        // Setup Firebase
        Firebase.remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
        Firebase.remoteConfig.setConfigSettingsAsync(remoteConfigSettings {
            minimumFetchIntervalInSeconds = 3600
        })

        // Add cache for logging
        Timber.plant(BaseInjector.get().timberCacheTree())
    }
}