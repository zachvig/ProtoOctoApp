package de.crysxd.octoapp.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.utils.AppScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit


class PrintNotificationSupportBroadcastReceiver() : BroadcastReceiver() {

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
                Timber.e(e)
            }
        }
    }

    private fun handleDisablePrintNotificationUntilNextLaunch(context: Context) {
        Timber.i("Stopping notification until next launch")
        Injector.get().octoPreferences().wasPrintNotificationDisabledUntilNextLaunch = true
        PrintNotificationManager.stop(context)
        PrintNotificationController.instance.cancelUpdateNotifications()
    }

    private suspend fun handleScreenOff(context: Context) {
        if (Injector.get().octoPreferences().allowNotificationBatterySaver) {
            if (PrintNotificationManager.isNotificationShowing) {
                pauseJob = AppScope.launch {
                    val delaySecs = 30L
                    Timber.i("Screen off, pausing notification in ${delaySecs}s")
                    delay(TimeUnit.SECONDS.toMillis(delaySecs))
                    Timber.i("Pausing notification")
                    PrintNotificationManager.pause(context)
                }
            }
        } else {
            Timber.d("Battery saver disabled, no action on screen off")
        }
    }

    private fun handleScreenOn(context: Context) {
        if (Injector.get().octoPreferences().allowNotificationBatterySaver) {
            pauseJob?.let {
                pauseJob = null
                Timber.i("Cancelling notification pause")
                it.cancel()
            }
            PrintNotificationManager.resume(context)
        } else {
            Timber.d("Battery saver disabled, no action on screen on")
        }
    }

    private suspend fun handleConnectionChange(context: Context) {
        val wasDisconnected = Injector.get().octoPreferences().wasPrintNotificationDisconnected
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val hasWifi = manager.allNetworks.map { manager.getNetworkCapabilities(it) }.any { it?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true }

        if (wasDisconnected && hasWifi) {
            Injector.get().octoPreferences().wasPrintNotificationDisconnected = false
            Timber.i("Wifi now connected. Print notification was disconnected before, attempting to reconnect in 5s")

            // Delay for 5s to get the network settled and then connect
            delay(5000)
            PrintNotificationManager.start(context)

            // If WiFi got reconnected, the local URL could also be reachable again. Perform online check.
            Injector.get().octoPrintProvider().octoPrint().performOnlineCheck()
        } else {
            Timber.v("Not starting service (wasDisconnected=$wasDisconnected, hasWifi=$hasWifi)")
        }
    }
}