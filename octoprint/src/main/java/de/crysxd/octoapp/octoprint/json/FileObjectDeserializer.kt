package de.crysxd.octoapp.octoprint.json

import com.google.gson.*
import de.crysxd.octoapp.octoprint.models.files.FileObject
import java.lang.reflect.Type

class FileObjectDeserializer(val gson: Gson) : JsonDeserializer<FileObject> {

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): FileObject {
        return when (json.asJsonObject["type"].asString) {
            "folder" -> gson.fromJson(json, FileObject.Folder::class.java)
            else -> gson.fromJson(json, FileObject.File::class.java)
        }
    }
}