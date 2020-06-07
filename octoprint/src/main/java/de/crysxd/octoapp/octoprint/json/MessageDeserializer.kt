package de.crysxd.octoapp.octoprint.json

import com.google.gson.*
import de.crysxd.octoapp.octoprint.models.socket.Message
import java.lang.reflect.Type

class MessageDeserializer(val gson: Gson) : JsonDeserializer<Message> {

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Message {

        val o = json.asJsonObject
        return when {
            o.has("current") -> gson.fromJson(json, Message.CurrentMessage::class.java)
            o.has("connected") -> gson.fromJson(json, Message.ConnectedMessage::class.java)
            o.has("plugin") -> Message.PluginMessage(o["plugin"].asJsonObject)
            else -> Message.RawMessage(o)
        }
    }
}