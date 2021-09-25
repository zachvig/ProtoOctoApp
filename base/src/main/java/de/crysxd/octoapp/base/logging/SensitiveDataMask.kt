package de.crysxd.octoapp.base.logging

import de.crysxd.octoapp.base.data.models.OctoPrintInstanceInformationV3
import de.crysxd.octoapp.octoprint.models.settings.Settings
import de.crysxd.octoapp.octoprint.redactLoggingString
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class SensitiveDataMask {

    private val lock = ReentrantLock()
    private val masks = mutableMapOf<String, (String) -> String>()

    fun registerWebUrl(webUrl: HttpUrl?) = lock.withLock {
        masks[webUrl.toString()] = {
            webUrl?.redactLoggingString(it) ?: it
        }
    }

    fun registerApiKey(apiKey: String) = lock.withLock {
        if (apiKey.isNotBlank()) {
            masks[apiKey] = {
                redact(it, apiKey, "api_key")
            }
        }
    }

    fun registerInstance(instance: OctoPrintInstanceInformationV3) {
        registerWebUrl(instance.webUrl)
        registerWebUrl(instance.alternativeWebUrl)
        registerWebUrl(instance.settings?.webcam?.streamUrl?.toHttpUrlOrNull())
        registerApiKey(instance.apiKey)
        instance.settings?.plugins?.values?.mapNotNull { it as? Settings.MultiCamSettings }?.firstOrNull()?.profiles?.forEachIndexed { i, webcam ->
            registerWebUrl(webcam.streamUrl?.toHttpUrlOrNull())
        }
    }

    fun mask(input: String): String = lock.withLock {
        var output = input

        masks.forEach {
            output = it.value(output)
        }

        return output
    }

    private fun redact(input: String, sensitiveData: String, replacement: String): String = input.replace(sensitiveData, "\${${replacement}}")

    data class SensitiveData(
        val sensitiveData: String,
        val replacement: String
    )
}