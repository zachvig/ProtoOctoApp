package de.crysxd.octoapp.octoprint.ext

import okhttp3.Call
import okhttp3.EventListener
import okhttp3.OkHttpClient
import java.net.InetAddress
import java.security.KeyStore
import java.security.SecureRandom
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

fun OkHttpClient.Builder.withSslKeystore(keyStore: KeyStore?): OkHttpClient.Builder {
    keyStore?.let { ks ->
        val customTrustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).also {
            it.init(ks)
        }
        val x509TrustManager = customTrustManagerFactory.trustManagers.mapNotNull {
            it as? X509TrustManager
        }.first()

        val sslContext = SSLContext.getInstance("SSL")
        val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        keyManagerFactory.init(keyStore, "pass".toCharArray())
        sslContext.init(keyManagerFactory.keyManagers, customTrustManagerFactory.trustManagers, SecureRandom())
        sslSocketFactory(sslContext.socketFactory, x509TrustManager)
    }
    return this
}

fun OkHttpClient.Builder.withHostnameVerifier(hostnameVerifier: HostnameVerifier?): OkHttpClient.Builder {
    hostnameVerifier?.let {
        hostnameVerifier(it)
    }
    return this
}

fun OkHttpClient.Builder.setDnsLoggingEnabled(enabled: Boolean) {
    eventListener(
        object : EventListener() {
            override fun dnsStart(call: Call, domainName: String) {
                super.dnsStart(call, domainName)
            }

            override fun dnsEnd(call: Call, domainName: String, inetAddressList: List<InetAddress>) {
                super.dnsEnd(call, domainName, inetAddressList)
            }
        }
    )
}