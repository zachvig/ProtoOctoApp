package de.crysxd.octoapp.base.network

import okhttp3.Dns

interface LocalDnsResolver : Dns {
    fun addUpnpDeviceToCache(upnpService: OctoPrintUpnpDiscovery.Service)
    fun addMDnsDeviceToCache(mDnsService: OctoPrintDnsSdDiscovery.Service)
}