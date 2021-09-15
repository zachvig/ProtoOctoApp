package de.crysxd.octoapp.notification

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.utils.AppScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

object LivePrintNotificationManager {
    val isNotificationEnabled
        get() = Injector.get().octoPreferences().isLivePrintNotificationsEnabled &&
                !Injector.get().octoPreferences().wasPrintNotificationDisabledUntilNextLaunch
    val isNotificationShowing get() = startTime > 0
    internal var startTime = 0L

    fun start(context: Context) {
        if (isNotificationEnabled) {
            // Already running?
            if (isNotificationShowing) return

            startTime = System.currentTimeMillis()
            val intent = Intent(context, PrintNotificationService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && context !is Activity) {
                Timber.i("Starting notification service as foreground")
                context.startForegroundService(intent)
            } else {
                Timber.i("Starting notification service")
                context.startService(intent)
            }
        } else {
            Timber.i("Skipping notification service start, disabled")

        }
    }

    fun stop(context: Context) = AppScope.launch {
        if (isNotificationShowing) {
            // We have issues with starting the service and then stopping it right after. After we started it as a foreground service,
            // we need to give it time to start and call startForeground(). Without this call being done, the app will crash, even if the service is already stopped
            val delay = 500 - (System.currentTimeMillis() - startTime).coerceAtMost(500)
            Timber.i("Stopping notification service after delay of ${delay}ms")
            delay(delay)
            val intent = Intent(context, PrintNotificationService::class.java)
            context.stopService(intent)
            startTime = 0
        }
    }

    fun restart(context: Context) {
        if (isNotificationShowing) {
            Injector.get().octoPreferences().wasPrintNotificationPaused = true
            stop(context)
            resume(context)
        }
    }

    fun pause(context: Context) {
        val isPausingEnabled = Injector.get().octoPreferences().allowNotificationBatterySaver
        val wasDisconnected = Injector.get().octoPreferences().wasPrintNotificationDisconnected
        if (isPausingEnabled && (wasDisconnected || isNotificationShowing)) {
            Timber.i("Pausing service")
            Injector.get().octoPreferences().wasPrintNotificationPaused = true
            stop(context)
        }
    }

    fun resume(context: Context) {
        if (Injector.get().octoPreferences().wasPrintNotificationPaused) {
            Timber.i("Resuming service")
            start(context)
        }
    }
}