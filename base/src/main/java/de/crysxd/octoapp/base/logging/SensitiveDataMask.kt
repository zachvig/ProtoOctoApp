package de.crysxd.octoapp.base.logging

import android.net.Uri
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class SensitiveDataMask {

    private val lock = ReentrantLock()
    private var sensitiveData = mutableListOf<SensitiveData>()

    fun registerWebUrl(webUrl: String) {
        try {
            val uri = Uri.parse(webUrl)
            registerSensitiveData(uri.host ?: webUrl, "octoprint_host")
            uri.userInfo?.let {
                registerSensitiveData(it, "octoprint_user_info")
            }
        } catch (e: Exception) {
            registerSensitiveData(webUrl, "octoprint_host")
        }
    }

    fun registerWebcamUrl(webUrl: String) {
        try {
            val uri = Uri.parse(webUrl)
            uri.host?.let {
                registerSensitiveData(uri.host ?: webUrl, "webcam_host")
            }
            uri.userInfo?.let {
                registerSensitiveData(it, "webcam_user_info")
            }
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

        return output
    }

    data class SensitiveData(
        val sensitiveData: String,
        val replacement: String
    )
}