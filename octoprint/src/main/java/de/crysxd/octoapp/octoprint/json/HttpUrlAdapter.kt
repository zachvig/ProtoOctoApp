package de.crysxd.octoapp.octoprint.json

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

class HttpUrlAdapter : TypeAdapter<HttpUrl>() {
    override fun write(out: JsonWriter, value: HttpUrl?) {
        out.value(value?.toString())
    }

    override fun read(reader: JsonReader): HttpUrl? {
        return reader.nextString().toHttpUrlOrNull()
    }
}