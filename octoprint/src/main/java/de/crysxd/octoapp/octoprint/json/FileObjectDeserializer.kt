package de.crysxd.octoapp.octoprint.json

import com.google.gson.*
import de.crysxd.octoapp.octoprint.models.files.FileObject
import java.lang.reflect.Type

class FileObjectDeserializer(val gson: Gson) : JsonDeserializer<FileObject> {

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): FileObject {
        val typedGson = gson.newBuilder().registerTypeAdapter(FileObject::class.java, this).create()

        return when (json.asJsonObject["type"].asString) {
            FileObject.FILE_TYPE_FOLDER -> typedGson.fromJson(json, FileObject.Folder::class.java)
            else -> typedGson.fromJson(json, FileObject.File::class.java)
        }
    }
}