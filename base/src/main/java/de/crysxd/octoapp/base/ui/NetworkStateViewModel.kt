package de.crysxd.octoapp.base.ui

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import timber.log.Timber

@SuppressLint("MissingPermission")
class NetworkStateViewModel(
    application: Application
) : ViewModel() {

    private val mutableNetworkState = MutableLiveData<NetworkState>()
    val networkState = mutableNetworkState.map { it }

    private val manager = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            updateNetworkState()
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            updateNetworkState()
        }
    }

    init {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            manager.registerDefaultNetworkCallback(networkCallback)
            updateNetworkState()
        } else {
            mutableNetworkState.postValue(NetworkState.Unknown)
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            manager.unregisterNetworkCallback(networkCallback)
        } catch (e: IllegalArgumentException) {
            // Old SDK or other hickup, manager does not know networkCallback
        }
    }

    private fun updateNetworkState() {
        val wifiConnected = manager.allNetworks.any { manager.getNetworkCapabilities(it)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true }
        Timber.i("Update network state: $wifiConnected")
        mutableNetworkState.postValue(if (wifiConnected) NetworkState.WifiConnected else NetworkState.WifiNotConnected)
    }

    sealed class NetworkState {
        object Unknown : NetworkState()
        object WifiConnected : NetworkState()
        object WifiNotConnected : NetworkState()
    }
}