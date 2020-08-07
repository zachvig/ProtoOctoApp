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
            val string = json.asString
                .toUpperCase(Locale.ENGLISH)
                .replace(" ", "_")
                .replace(":", "")

            when {
                string.startsWith("ERROR_FAILED_TO_AUTODETECT_SERIAL_PORT") -> ConnectionResponse.ConnectionState.ERROR_FAILED_TO_AUTODETECT_SERIAL_PORT
                string.startsWith("ERROR_CONNECTION_ERROR") -> ConnectionResponse.ConnectionState.CONNECTION_ERROR
                string.contains("ERROR") -> ConnectionResponse.ConnectionState.UNKNOWN_ERROR
                else -> ConnectionResponse.ConnectionState.valueOf(string)
            }
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Unable to deserialize '$json'", e)
            ConnectionResponse.ConnectionState.UNKNOWN
        }
    }
}