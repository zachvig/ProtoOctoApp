package de.crysxd.octoapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.os.Handler
import android.provider.Settings
import androidx.core.content.res.ResourcesCompat
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import de.crysxd.octoapp.base.OctoAnalytics
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

        // Setup SerialCommunicationLogsRepository (jsut create the instance)
        BaseInjector.get().serialCommunicationLogsRepository()

        // Add cache for logging and report to firebase
        Timber.plant(BaseInjector.get().timberCacheTree())
        Timber.plant(BaseInjector.get().firebaseTree())

        // Setup RemoteConfig
        Firebase.remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
        Firebase.remoteConfig.setConfigSettingsAsync(remoteConfigSettings {
            minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) 10 else 3600
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

        // Setup FCM
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Timber.tag("FCM").i("Token: ${task.result}")
            } else {
                Timber.tag("FCM").w("Unable to get token")
            }
        }

        // Create anonymous user
        if (Firebase.auth.currentUser == null) {
            Firebase.auth.signInAnonymously().addOnSuccessListener {
                Timber.i("Signed in anonymously as ${it.user?.uid}")
                OctoAnalytics.setUserProperty(OctoAnalytics.UserProperty.UserId, it.user?.uid)
            }.addOnFailureListener {
                Timber.e("Failed to sign in: $it")
            }
        } else {
            Timber.i("Already signed in as ${Firebase.auth.currentUser?.uid}")
            OctoAnalytics.setUserProperty(OctoAnalytics.UserProperty.UserId, Firebase.auth.currentUser?.uid)
        }

        // Setup analytics
        // Do not enable if we are in a TestLab environment
        val testLabSetting = Settings.System.getString(contentResolver, "firebase.test.lab")
        Firebase.analytics.setAnalyticsCollectionEnabled("true" != testLabSetting && !BuildConfig.DEBUG)
        if (BuildConfig.DEBUG) {
            Firebase.analytics.setUserProperty("debug", "true")
        }

        // Pre-load fonts in background. This will allow us later to asyn inflate views as loading fonts will need a Handler
        // After being loaded once, they are in cache
        val handler = Handler()
        val callback = object : ResourcesCompat.FontCallback() {
            override fun onFontRetrievalFailed(reason: Int) = Unit
            override fun onFontRetrieved(typeface: Typeface) = Unit
        }
        ResourcesCompat.getFont(this, R.font.roboto_medium, callback, handler)
        ResourcesCompat.getFont(this, R.font.roboto_light, callback, handler)
        ResourcesCompat.getFont(this, R.font.roboto_regular, callback, handler)
    }
}