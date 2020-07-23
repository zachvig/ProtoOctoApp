package de.crysxd.octoapp.octoprint

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import de.crysxd.octoapp.octoprint.api.*
import de.crysxd.octoapp.octoprint.exceptions.GenerateExceptionInterceptor
import de.crysxd.octoapp.octoprint.json.ConnectionStateDeserializer
import de.crysxd.octoapp.octoprint.json.FileObjectDeserializer
import de.crysxd.octoapp.octoprint.json.MessageDeserializer
import de.crysxd.octoapp.octoprint.models.connection.ConnectionResponse
import de.crysxd.octoapp.octoprint.models.files.FileObject
import de.crysxd.octoapp.octoprint.models.socket.Message
import de.crysxd.octoapp.octoprint.websocket.EventWebSocket
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class OctoPrint(
    private val hostName: String,
    private val port: Int,
    private val apiKey: String,
    private val interceptors: List<Interceptor> = emptyList()
) {

    private val webSocket = EventWebSocket(createOkHttpClient(), hostName, port, createGsonWithTypeAdapters())

    fun getEventWebSocket() = webSocket

    fun createVersionApi(): VersionApi =
        createRetrofit().create(VersionApi::class.java)

    fun createJobApi(): JobApi.Wrapper =
        JobApi.Wrapper(createRetrofit().create(JobApi::class.java), webSocket)

    fun createFilesApi(): FilesApi.Wrapper =
        FilesApi.Wrapper(createRetrofit().create(FilesApi::class.java))

    fun createPrinterApi(): PrinterApi.Wrapper =
        PrinterApi.Wrapper(createRetrofit().create(PrinterApi::class.java), webSocket)

    fun createPsuApi(): PsuApi.Wrapper =
        PsuApi.Wrapper((createRetrofit().create(PsuApi::class.java)), webSocket)

    fun createConnectionApi(): ConnectionApi.Wrapper =
        ConnectionApi.Wrapper((createRetrofit().create(ConnectionApi::class.java)))

    fun gerWebUrl(): String = "http://${hostName}:${port}/"

    private fun createRetrofit() = Retrofit.Builder()
        .baseUrl("${gerWebUrl()}api/")
        .addConverterFactory(GsonConverterFactory.create(createGsonWithTypeAdapters()))
        .client(createOkHttpClient())
        .build()

    private fun createGsonWithTypeAdapters(): Gson = createBaseGson().newBuilder()
        .registerTypeAdapter(ConnectionResponse.ConnectionState::class.java, ConnectionStateDeserializer())
        .registerTypeAdapter(FileObject::class.java, FileObjectDeserializer(createBaseGson()))
        .registerTypeAdapter(Message::class.java, MessageDeserializer(createBaseGson()))
        .create()

    private fun createBaseGson(): Gson = GsonBuilder()
        .create()

    private fun createOkHttpClient() = OkHttpClient.Builder().apply {
        addInterceptor(createAddHeaderInterceptor())
        addInterceptor(GenerateExceptionInterceptor())
        this@OctoPrint.interceptors.forEach { addInterceptor(it) }
    }.build()

    private fun createAddHeaderInterceptor() = Interceptor {
        it.proceed(
            it.request().newBuilder()
                .addHeader("X-Api-Key", apiKey)
                .addHeader("Accept-Encoding", "application/json")
                .build()
        )
    }
}