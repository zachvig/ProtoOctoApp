package de.crysxd.octoapp

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
        context.registerReceiver(this, intentFilter)
    }

    override fun onReceive(context: Context, intent: Intent) {
        GlobalScope.launch {
            try {
                val wasDisconnected = Injector.get().octoPreferences().wasPrintNotificationDisconnected
                val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val hasWifi = manager.allNetworks.map { manager.getNetworkCapabilities(it) }.any { it?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true }

                if (wasDisconnected && hasWifi) {
                    Injector.get().octoPreferences().wasPrintNotificationDisconnected = false
                    Timber.i("Network state changed, wifi now connected. Print notification was disconnected before, attempting to reconnect in 5s")

                    // Delay for 5s to get the network settled and then connect
                    delay(5000)
                    PrintNotificationService.start(context)
                } else {
                    Timber.v("Not starting service (wasDisconnected=$wasDisconnected, hasWifi=$hasWifi)")
                }
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }
}