package de.crysxd.octoapp

import android.app.Application
import de.crysxd.octoapp.signin.di.Injector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.net.InetAddress

class OctoApp : Application() {

    override fun onCreate() {
        super.onCreate()

        Timber.plant(Timber.DebugTree())
        Injector.init(this)
    }
}