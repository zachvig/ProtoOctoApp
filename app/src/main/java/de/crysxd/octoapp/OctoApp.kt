package de.crysxd.octoapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.provider.Settings
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.iid.FirebaseInstanceId
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

        // Setup analytics
        // Do not enable if we are in a TestLab environment
        val testLabSetting: String = Settings.System.getString(contentResolver, "firebase.test.lab")
        Firebase.analytics.setAnalyticsCollectionEnabled("true" != testLabSetting)

        // Setup logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        val wrapped = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            Timber.tag("Uncaught!").wtf(e)
            wrapped?.uncaughtException(t, e)
        }

        // Setup Dagger
        BaseInjector.init(this)
        SignInInjector.init(BaseInjector.get())
        ConnectPrintInjector.init(BaseInjector.get())
        PrePrintControlsInjector.init(BaseInjector.get())
        PrintControlsInjector.init(BaseInjector.get())

        // Add cache for logging and report to firebase
        Timber.plant(BaseInjector.get().timberCacheTree())
        Timber.plant(BaseInjector.get().firebaseTree())

        // Setup Firebase
        Firebase.remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
        Firebase.remoteConfig.setConfigSettingsAsync(remoteConfigSettings {
            minimumFetchIntervalInSeconds = 3600
        })
        Firebase.remoteConfig.fetchAndActivate().addOnCompleteListener {
            it.exception?.let(Timber::e)
            Timber.i("Complete remote config fetch (success=${it.isSuccessful})")
        }

        // Register default notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(
                NotificationChannel(
                    getString(R.string.updates_notification_channel),
                    getString(R.string.updates_notification_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
        }

        FirebaseInstanceId.getInstance().instanceId.addOnCompleteListener(OnCompleteListener { task ->
            if (task.isSuccessful) {
                Timber.tag("FCM").i("Token: ${task.result?.token}")
            } else {
                Timber.tag("FCM").w("Unable to get token")
            }
        })
    }
}