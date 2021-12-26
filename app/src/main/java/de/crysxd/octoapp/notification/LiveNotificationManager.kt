package de.crysxd.octoapp.notification

import android.app.Activity
import android.app.BackgroundServiceStartNotAllowedException
import android.app.ForegroundServiceStartNotAllowedException
import android.content.Context
import android.content.Intent
import android.os.Build
import de.crysxd.octoapp.base.di.BaseInjector
import de.crysxd.octoapp.base.utils.AppScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

object LiveNotificationManager {
    val isNotificationEnabled
        get() = BaseInjector.get().octoPreferences().isLivePrintNotificationsEnabled &&
                !BaseInjector.get().octoPreferences().wasPrintNotificationDisabledUntilNextLaunch
    val isNotificationShowing get() = startTime > 0
    internal var startTime = 0L
        set(value) {
            if (value == 0L) isHibernating = false
            field = value
        }
    private var isHibernating = false

    fun pauseNotificationsUntilNextLaunch(context: Context) {
        Timber.i("Stopping notification until next launch")
        BaseInjector.get().octoPreferences().wasPrintNotificationDisabledUntilNextLaunch = true
        stop(context)
        PrintNotificationController.instance.cancelUpdateNotifications()
    }

    fun restartIfWasPaused(context: Context) {
        if (BaseInjector.get().octoPreferences().wasPrintNotificationDisabledUntilNextLaunch) {
            Timber.i("Restarting notification after it was paused")
            BaseInjector.get().octoPreferences().wasPrintNotificationDisabledUntilNextLaunch = false
            start(context)
        }
    }

    fun start(context: Context) {
        try {
            // Guard: We can't start the service without an active instance
            BaseInjector.get().octorPrintRepository().getActiveInstanceSnapshot()
                ?: return Timber.w("Rejecting start of service: No active instance")

            if (isNotificationEnabled) {
                // Already running?
                if (isHibernating) {
                    wakeUp(context)
                } else if (!isNotificationShowing) {
                    startTime = System.currentTimeMillis()
                    val intent = Intent(context, LiveNotificationService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && context !is Activity) {
                        Timber.i("Starting notification service as foreground")
                        context.startForegroundService(intent)
                    } else {
                        Timber.i("Starting notification service")
                        context.startService(intent)
                    }
                }
            } else {
                Timber.v("Skipping notification service start, disabled")
            }
        } catch (e: Exception) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && e is ForegroundServiceStartNotAllowedException) {
                Timber.w("Foreground service not allowed, can't start")
            } else {
                throw e
            }
        }
    }

    fun stop(context: Context) = AppScope.launch {
        if (isNotificationShowing) {
            // We have issues with starting the service and then stopping it right after. After we started it as a foreground service,
            // we need to give it time to start and call startForeground(). Without this call being done, the app will crash, even if the service is already stopped
            val delay = 500 - (System.currentTimeMillis() - startTime).coerceAtMost(500)
            Timber.i("Stopping notification service after delay of ${delay}ms")
            delay(delay)
            val intent = Intent(context, LiveNotificationService::class.java)
            context.stopService(intent)
            startTime = 0
        }
    }

    fun restart(context: Context) {
        if (isNotificationShowing) {
            stop(context)
            start(context)
        }
    }

    fun hibernate(context: Context) = runCatchingServiceExceptions {
        val isHibernationEnabled = BaseInjector.get().octoPreferences().allowNotificationBatterySaver
        if (isHibernationEnabled && isNotificationShowing) {
            Timber.i("Sending service into hibernation")
            isHibernating = true
            val intent = Intent(context, LiveNotificationService::class.java)
            intent.action = LiveNotificationService.ACTION_HIBERNATE
            context.startService(intent)
        }
    }

    fun wakeUp(context: Context) = runCatchingServiceExceptions {
        if (isHibernating) {
            Timber.i("Resuming service")
            isHibernating = false
            val intent = Intent(context, LiveNotificationService::class.java)
            intent.action = LiveNotificationService.ACTION_WAKE_UP
            context.startService(intent)
        }
    }

    private fun runCatchingServiceExceptions(block: () -> Unit) = try {
        block()
    } catch (e: java.lang.Exception) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && e is BackgroundServiceStartNotAllowedException) {
            Timber.w("Unable to perform hibernation, background service not allowed")
        } else {
            throw e
        }
    }
}