package de.crysxd.octoapp.octoprint.json

import com.google.gson.Gson
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import de.crysxd.octoapp.octoprint.models.job.ProgressInformation
import java.lang.reflect.Type

class ProgressInformationDeserializer(
    private val baseGson: Gson
) : JsonDeserializer<ProgressInformation> {

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ProgressInformation {
        val base = baseGson.fromJson(json, ProgressInformation::class.java)
        return if (base.printTimeLeftOrigin == ProgressInformation.ORIGIN_GENIUS) {
            // The PrintTimeGenius plugin changes the progress in the webinterface to be calculated as below
            // We need to do the same here to keep the data consistent for the user
            base.copy(completion = (base.printTime / (base.printTime + base.printTimeLeft.toFloat())) * 100f)
        } else {
            base
        }
    }
}