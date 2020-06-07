package de.crysxd.octoapp.octoprint.json

import com.google.gson.*
import de.crysxd.octoapp.octoprint.models.files.FileOrigin
import de.crysxd.octoapp.octoprint.models.socket.Message
import java.lang.reflect.Type
import java.util.*

class MessageDeserializer(val gson: Gson) : JsonDeserializer<Message> {

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Message {

        val o = json.asJsonObject
        return when {
            o.has("current") -> gson.fromJson(o["current"], Message.CurrentMessage::class.java)
            o.has("history") -> gson.fromJson(o["history"], Message.CurrentMessage::class.java)
            o.has("connected") -> gson.fromJson(o["connected"], Message.ConnectedMessage::class.java)
            o.has("plugin") -> Message.PluginMessage(o["plugin"].asJsonObject)
            o.has("event") -> deserializeEventMessage(o["event"].asJsonObject)
            else -> Message.RawMessage(o)
        }
    }

    private fun deserializeEventMessage(o: JsonObject): Message.EventMessage = when (o["type"].asString) {
        "PrinterStateChanged" -> {
            Message.EventMessage.PrinterStateChanged(
                o["payload"].asJsonObject["state_string"].asString,
                mapPrinterStateId(o["payload"].asJsonObject["state_id"].asString)
            )
        }

        "Connecting" -> Message.EventMessage.Connecting

        "Connected" -> Message.EventMessage.Connected(
            o["payload"].asJsonObject["baudrate"].asInt,
            o["payload"].asJsonObject["port"].asString
        )

        "Disconnected" -> Message.EventMessage.Disconnected

        "PrintStarted" -> deserializeFileEventMessage(Message.EventMessage.PrintStarted::class.java, o["payload"].asJsonObject)

        "FileSelected" -> deserializeFileEventMessage(Message.EventMessage.FileSelected::class.java, o["payload"].asJsonObject)

        "PrintCancelling" -> deserializeFileEventMessage(Message.EventMessage.PrintCancelling::class.java, o["payload"].asJsonObject)

        "PrintCancelled" -> deserializeFileEventMessage(Message.EventMessage.PrintCancelled::class.java, o["payload"].asJsonObject)

        "PrintPausing" -> deserializeFileEventMessage(Message.EventMessage.PrintPausing::class.java, o["payload"].asJsonObject)

        "PrintPaused" -> deserializeFileEventMessage(Message.EventMessage.PrintPaused::class.java, o["payload"].asJsonObject)

        "PrintFailed" -> deserializeFileEventMessage(Message.EventMessage.PrintFailed::class.java, o["payload"].asJsonObject)

        else -> {
            Message.EventMessage.Unknown
        }
    }

    private fun deserializeFileEventMessage(type: Class<out Message.EventMessage>, jsonObject: JsonObject): Message.EventMessage {
        val constructor = type.getConstructor(FileOrigin::class.java, String::class.java, String::class.java)
        val origin = if (jsonObject["origin"].asString == FileOrigin.SdCard.toString()) {
            FileOrigin.SdCard
        } else {
            FileOrigin.Local
        }
        val name = jsonObject["name"].asString
        val path = jsonObject["path"].asString
        return constructor.newInstance(origin, name, path)
    }

    private fun mapPrinterStateId(id: String) = try {
        Message.EventMessage.PrinterStateChanged.PrinterState.valueOf(id.toUpperCase(Locale.ENGLISH))
    } catch (e: Exception) {
        Message.EventMessage.PrinterStateChanged.PrinterState.UNKNOWN
    }
}