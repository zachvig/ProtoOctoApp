package de.crysxd.octoapp.base.network

import okhttp3.Call
import okhttp3.EventListener
import okhttp3.Protocol
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy

class NetworkDebugLogger : EventListener() {

    private val Timber get() = timber.log.Timber.tag("HTTP/debug")

    override fun dnsStart(call: Call, domainName: String) {
        super.dnsStart(call, domainName)
        Timber.d("[${call.request().url}] DNS start for $domainName")
    }

    override fun dnsEnd(call: Call, domainName: String, inetAddressList: List<InetAddress>) {
        super.dnsEnd(call, domainName, inetAddressList)
        Timber.d("[${call.request().url}] DNS end for $domainName: $inetAddressList")
    }

    override fun callStart(call: Call) {
        super.callStart(call)
        Timber.d("[${call.request().url}] Started")
    }

    override fun callEnd(call: Call) {
        super.callStart(call)
        Timber.d("[${call.request().url}] Ended")
    }

    override fun callFailed(call: Call, ioe: IOException) {
        super.callFailed(call, ioe)
        Timber.d("[${call.request().url}] Failed: exception=${ioe::class.simpleName}: ${ioe.message}")
    }

    override fun connectStart(call: Call, inetSocketAddress: InetSocketAddress, proxy: Proxy) {
        super.connectStart(call, inetSocketAddress, proxy)
        Timber.d("[${call.request().url}] Connection setup start: inetSocketAddress=$inetSocketAddress proxy=$proxy")
    }

    override fun connectEnd(call: Call, inetSocketAddress: InetSocketAddress, proxy: Proxy, protocol: Protocol?) {
        super.connectEnd(call, inetSocketAddress, proxy, protocol)
        Timber.d("[${call.request().url}] Connection setup end: inetSocketAddress=$inetSocketAddress proxy=$proxy")
    }

    override fun connectFailed(call: Call, inetSocketAddress: InetSocketAddress, proxy: Proxy, protocol: Protocol?, ioe: IOException) {
        super.connectFailed(call, inetSocketAddress, proxy, protocol, ioe)
        Timber.d("[${call.request().url}] Connection setup failed: inetSocketAddress=$inetSocketAddress proxy=$proxy protocol=$protocol exception=${ioe::class.simpleName}: ${ioe.message}")
    }
}