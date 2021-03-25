package de.crysxd.octoapp.octoprint.json

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import de.crysxd.octoapp.octoprint.models.printer.PrinterState
import de.crysxd.octoapp.octoprint.models.socket.HistoricTemperatureData
import java.lang.reflect.Type

class HistoricTemperatureDeserializer : JsonDeserializer<HistoricTemperatureData> {

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): HistoricTemperatureData {
        val time = json.asJsonObject["time"].asLong
        val components = json.asJsonObject.keySet().filter {
            // If chamber is not active, it's still included byt the actual value is a "null" -> filter out
            it != "time" && !json.asJsonObject[it].asJsonObject["actual"].isJsonNull
        }.map {
            it to context.deserialize<PrinterState.ComponentTemperature>(json.asJsonObject[it], PrinterState.ComponentTemperature::class.java)
        }.toMap()

        return HistoricTemperatureData(
            time = time,
            components = components
        )
    }
}