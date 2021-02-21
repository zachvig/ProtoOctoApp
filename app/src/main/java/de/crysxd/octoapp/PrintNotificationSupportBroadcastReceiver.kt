package de.crysxd.octoapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import de.crysxd.octoapp.base.di.Injector
import timber.log.Timber


class PrintNotificationSupportBroadcastReceiver(context: Context) : BroadcastReceiver() {

    init {
        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
        context.registerReceiver(this, intentFilter)
    }

    override fun onReceive(context: Context, intent: Intent) {
        try {
            val wasDisconnected = Injector.get().octoPreferences().wasPrintNotificationDisconnected
            Injector.get().octoPreferences().wasPrintNotificationDisconnected = false
            val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val hasWifi = manager.allNetworks.map { manager.getNetworkCapabilities(it) }.any { it?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true }

            if (wasDisconnected && hasWifi) {
                Timber.i("Network state changed, wifi now connected. Print notification was disconnected before, attempting reconnect")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(Intent(context, PrintNotificationService::class.java))
                } else {
                    context.startService(Intent(context, PrintNotificationService::class.java))
                }
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }
}