package de.crysxd.octoapp.octoprint.json

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import de.crysxd.octoapp.octoprint.models.settings.Settings
import java.lang.reflect.Type

class PluginSettingsDeserializer : JsonDeserializer<Settings.PluginSettingsGroup> {

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): Settings.PluginSettingsGroup {
        val obj = json.asJsonObject
        val settings = obj.keySet().toList().map {
            Pair(it, deserialize(context, it, obj.get(it)))
        }.toMap()

        return Settings.PluginSettingsGroup(settings)
    }

    private fun deserialize(context: JsonDeserializationContext, key: String, element: JsonElement) = when (key) {
        "gcodeviewer" -> context.deserialize<Settings.GcodeViewerSettings>(element, Settings.GcodeViewerSettings::class.java)
        else -> object : Settings.PluginSettings {}
    }
}