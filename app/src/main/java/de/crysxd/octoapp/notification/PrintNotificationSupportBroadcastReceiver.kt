package de.crysxd.octoapp.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import de.crysxd.octoapp.base.di.Injector
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber


class PrintNotificationSupportBroadcastReceiver(context: Context) : BroadcastReceiver() {

    init {
        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
        intentFilter.addAction(Intent.ACTION_SCREEN_ON)
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF)
        context.registerReceiver(this, intentFilter)
    }

    override fun onReceive(context: Context, intent: Intent) {
        GlobalScope.launch {
            try {
                when (intent.action) {
                    WifiManager.NETWORK_STATE_CHANGED_ACTION -> handleConnectionChange(context)
                    Intent.ACTION_SCREEN_OFF -> handleScreenOff(context)
                    Intent.ACTION_SCREEN_ON -> handleScreenOn(context)
                }
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    private suspend fun handleScreenOff(context: Context) {

    }

    private suspend fun handleScreenOn(context: Context) {

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