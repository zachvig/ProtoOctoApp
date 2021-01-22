package de.crysxd.octoapp.base.logging

import android.net.Uri
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class SensitiveDataMask {

    private val lock = ReentrantLock()
    private var sensitiveData = mutableListOf<SensitiveData>()
    private val basicAuthRegex = Regex("(http[s]?)://(.*)@")

    fun registerWebUrl(webUrl: String) {
        try {
            registerSensitiveData(Uri.parse(webUrl).host ?: webUrl, "octoprint_host")
        } catch (e: Exception) {
            registerSensitiveData(webUrl, "octoprint_host")
        }
    }

    fun registerApiKey(apiKey: String) {
        registerSensitiveData(apiKey, "api_key")
    }

    private fun registerSensitiveData(data: String, replacement: String) = lock.withLock {
        val d = SensitiveData(data, replacement)
        if (!sensitiveData.contains(d)) {
            sensitiveData.add(d)
        }
    }

    fun mask(input: String): String = lock.withLock {
        var output = input

        sensitiveData.forEach {
            output = output.replace(it.sensitiveData, "\${${it.replacement}}")
        }

        if (output.contains("@")) {
            output = output.replace(basicAuthRegex, "$1://\\\${user}:\\\${password}@")
        }

        return output
    }

    data class SensitiveData(
        val sensitiveData: String,
        val replacement: String
    )
}