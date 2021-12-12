package de.crysxd.octoapp.notification

import android.app.BackgroundServiceStartNotAllowedException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import de.crysxd.octoapp.base.di.BaseInjector
import de.crysxd.octoapp.base.utils.AppScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.TimeUnit


class PrintNotificationSupportBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private var pauseJob: Job? = null
        const val ACTION_DISABLE_PRINT_NOTIFICATION_UNTIL_NEXT_LAUNCH = "de.crysxd.octoapp.ACTION_DISABLE_PRINT_NOTIFICATION_UNTIL_NEXT_LAUNCH"
    }

    fun install(context: Context) {
        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
        intentFilter.addAction(Intent.ACTION_SCREEN_ON)
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF)
        intentFilter.addAction(ACTION_DISABLE_PRINT_NOTIFICATION_UNTIL_NEXT_LAUNCH)
        context.registerReceiver(this, intentFilter)
    }

    override fun onReceive(context: Context, intent: Intent) {
        AppScope.launch {
            Timber.v("Handling ${intent.action}")
            try {
                when (intent.action) {
                    WifiManager.NETWORK_STATE_CHANGED_ACTION -> handleConnectionChange(context)
                    Intent.ACTION_SCREEN_OFF -> handleScreenOff(context)
                    Intent.ACTION_SCREEN_ON -> handleScreenOn(context)
                    ACTION_DISABLE_PRINT_NOTIFICATION_UNTIL_NEXT_LAUNCH -> handleDisablePrintNotificationUntilNextLaunch(context)
                }
            } catch (e: Exception) {
                // I don't dare to combine those two...let's keep it separate and secure
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (e is BackgroundServiceStartNotAllowedException) {
                        return@launch Timber.w(e, "Unable to process broadcast ${intent.action} as app is in background")
                    }
                }

                Timber.e(e)
            }
        }
    }

    private fun handleDisablePrintNotificationUntilNextLaunch(context: Context) {
        LiveNotificationManager.pauseNotificationsUntilNextLaunch(context)
    }

    private suspend fun handleScreenOff(context: Context) {
        if (BaseInjector.get().octoPreferences().allowNotificationBatterySaver) {
            if (LiveNotificationManager.isNotificationShowing) {
                pauseJob = AppScope.launch {
                    val delaySecs = 5L
                    Timber.i("Screen off, sending live notification into hibernation in ${delaySecs}s")
                    delay(TimeUnit.SECONDS.toMillis(delaySecs))
                    Timber.i("Sending live notification into hibernation")
                    LiveNotificationManager.hibernate(context)
                }
            }
        } else {
            Timber.d("Battery saver disabled, no action on screen off")
        }
    }

    private fun handleScreenOn(context: Context) {
        if (BaseInjector.get().octoPreferences().allowNotificationBatterySaver) {
            pauseJob?.let {
                pauseJob = null
                Timber.i("Cancelling notification hibernation")
                it.cancel()
            }
            LiveNotificationManager.wakeUp(context)
        } else {
            Timber.d("Battery saver disabled, no action on screen on")
        }
    }

    private suspend fun handleConnectionChange(context: Context) {
        val wasDisconnected = BaseInjector.get().octoPreferences().wasPrintNotificationDisconnected
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val hasWifi = manager.allNetworks.map { manager.getNetworkCapabilities(it) }.any { it?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true }
        val connectDelayMs = 5000L

        if (wasDisconnected && hasWifi) {
            BaseInjector.get().octoPreferences().wasPrintNotificationDisconnected = false
            Timber.i("Network changed. Print notification was disconnected before, attempting to reconnect in ${connectDelayMs / 1000}s")

            // Delay for 5s to get the network settled and then connect
            delay(connectDelayMs)
            LiveNotificationManager.start(context)

            // If WiFi got reconnected, the local URL could also be reachable again. Perform online check.
            withContext(Dispatchers.IO) {
                BaseInjector.get().octoPrintProvider().octoPrint().performOnlineCheck()
            }
        } else {
            Timber.v("Not starting service (wasDisconnected=$wasDisconnected, hasWifi=$hasWifi)")
        }
    }
}