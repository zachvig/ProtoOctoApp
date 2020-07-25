package de.crysxd.octoapp.octoprint.json

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import de.crysxd.octoapp.octoprint.models.connection.ConnectionResponse
import java.lang.reflect.Type
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger

class ConnectionStateDeserializer(
    private val logger: Logger
) : JsonDeserializer<ConnectionResponse.ConnectionState> {

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ConnectionResponse.ConnectionState {
        return try {
            ConnectionResponse.ConnectionState.valueOf(
                json.asString
                    .toUpperCase(Locale.ENGLISH)
                    .replace(" ", "_")
            )
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Unable to deserialize '$json'", e)
            ConnectionResponse.ConnectionState.UNKNOWN
        }
    }
}