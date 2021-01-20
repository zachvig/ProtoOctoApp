package de.crysxd.octoapp.base.logging

import android.net.Uri
import java.util.*

class SensitiveDataMask {

    private var sensitiveData = Vector<SensitiveData>()
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

    fun registerSensitiveData(data: String, replacement: String) {
        val data = SensitiveData(data, replacement)
        if (!sensitiveData.contains(data)) {
            sensitiveData.add(data)
        }
    }

    fun mask(input: String): String {
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