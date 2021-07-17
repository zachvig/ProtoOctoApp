package de.crysxd.octoapp.base.logging

import android.net.Uri
import de.crysxd.octoapp.base.network.OctoPrintUpnpDiscovery.Companion.UPNP_ADDRESS_PREFIX
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class SensitiveDataMask {

    private val lock = ReentrantLock()
    private var sensitiveData = mutableListOf<SensitiveData>()

    fun registerWebUrl(webUrl: String?, maskPrefix: String) {
        webUrl ?: return
        try {
            val uri = Uri.parse(webUrl)
            when {
                uri.host?.startsWith("192.168.") == true -> Unit
                uri.host?.endsWith(".lan") == true -> Unit
                uri.host?.endsWith(".local") == true -> Unit
                uri.host?.endsWith(".home") == true -> Unit
                uri.host?.startsWith(UPNP_ADDRESS_PREFIX) == true -> Unit
                else -> {
                    val value = uri.host ?: webUrl
                    val hash = value.hashCode().toString().take(4)
                    registerSensitiveData(value, "${maskPrefix}_host_$hash")
                }
            }
            uri.userInfo?.let {
                registerSensitiveData(it, "${maskPrefix}_user_info")
            }
        } catch (e: Exception) {
            registerSensitiveData(webUrl, "${maskPrefix}_host")
        }
    }

    fun registerApiKey(apiKey: String) {
        registerSensitiveData(apiKey, "api_key")
    }

    private fun registerSensitiveData(data: String, replacement: String) = lock.withLock {
        val d = SensitiveData(data, replacement)
        if (!sensitiveData.contains(d) && data.length >= 4) {
            sensitiveData.add(d)
        }
    }

    fun mask(input: String): String = lock.withLock {
        var output = input

        sensitiveData.forEach {
            output = output.replace(it.sensitiveData, "\${${it.replacement}}")
        }

        return output
    }

    data class SensitiveData(
        val sensitiveData: String,
        val replacement: String
    )
}