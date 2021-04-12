package de.crysxd.octoapp.base.logging

import android.net.Uri
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class SensitiveDataMask {

    private val lock = ReentrantLock()
    private var sensitiveData = mutableListOf<SensitiveData>()

    fun registerWebUrl(webUrl: String?, maskPrefix: String) {
        webUrl ?: return
        try {
            val uri = Uri.parse(webUrl)
            registerSensitiveData(uri.host ?: webUrl, "${maskPrefix}_host")
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