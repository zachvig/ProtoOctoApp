package de.crysxd.octoapp.notification

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import de.crysxd.octoapp.base.di.Injector
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

object PrintNotificationManager {
    val isNotificationEnabled get() = Injector.get().octoPreferences().isPrintNotificationEnabled
    private var startTime = 0L

    fun start(context: Context) {
        if (isNotificationEnabled) {
            // Already running?
            if (startTime > 0) return

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

    fun stop(context: Context) = GlobalScope.launch {
        if (startTime > 0) {
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
}