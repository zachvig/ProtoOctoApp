package de.crysxd.octoapp.octoprint

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import de.crysxd.octoapp.octoprint.api.*
import de.crysxd.octoapp.octoprint.exceptions.GenerateExceptionInterceptor
import de.crysxd.octoapp.octoprint.exceptions.OctoPrintException
import de.crysxd.octoapp.octoprint.json.ConnectionStateDeserializer
import de.crysxd.octoapp.octoprint.json.FileObjectDeserializer
import de.crysxd.octoapp.octoprint.json.MessageDeserializer
import de.crysxd.octoapp.octoprint.logging.LoggingInterceptorLogger
import de.crysxd.octoapp.octoprint.models.connection.ConnectionResponse
import de.crysxd.octoapp.octoprint.models.files.FileObject
import de.crysxd.octoapp.octoprint.models.socket.Message
import de.crysxd.octoapp.octoprint.websocket.EventWebSocket
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.net.URI
import java.util.logging.Logger


class OctoPrint(
    webUrl: String,
    private val apiKey: String,
    private val interceptors: List<Interceptor> = emptyList()
) {

    val webUrl = "${webUrl.removeSuffix("/")}/"

    companion object {
        const val TESTED_SERVER_VERSION = "1.4.2"
    }

    private val webSocket = EventWebSocket(
        httpClient = createOkHttpClient(),
        webUrl = webUrl,
        gson = createGsonWithTypeAdapters(),
        logger = getLogger(),
        loginApi = createLoginApi()
    )

    fun getEventWebSocket() = webSocket

    fun createLoginApi(): LoginApi =
        createRetrofit().create(LoginApi::class.java)

    fun createVersionApi(): VersionApi =
        createRetrofit().create(VersionApi::class.java)

    fun createSettingsApi(): SettingsApi =
        createRetrofit().create(SettingsApi::class.java)

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

    fun getLogger(): Logger = Logger.getLogger("OctoPrint")

    private fun createRetrofit() = Retrofit.Builder()
        .baseUrl(URI.create(webUrl).resolve("api/").toURL())
        .addConverterFactory(GsonConverterFactory.create(createGsonWithTypeAdapters()))
        .client(createOkHttpClient())
        .build()

    private fun createGsonWithTypeAdapters(): Gson = createBaseGson().newBuilder()
        .registerTypeAdapter(ConnectionResponse.ConnectionState::class.java, ConnectionStateDeserializer(getLogger()))
        .registerTypeAdapter(FileObject::class.java, FileObjectDeserializer(createBaseGson()))
        .registerTypeAdapter(Message::class.java, MessageDeserializer(getLogger(), createBaseGson()))
        .create()

    private fun createBaseGson(): Gson = GsonBuilder()
        .create()

    fun createOkHttpClient() = OkHttpClient.Builder().apply {
        val logger = Logger.getLogger("OctoPrint/HTTP")
        logger.parent = getLogger()
        logger.useParentHandlers = true

        addInterceptor(createCatchAllInterceptor())
        addInterceptor(createAddHeaderInterceptor())
        addInterceptor(GenerateExceptionInterceptor())
        addInterceptor(
            HttpLoggingInterceptor(LoggingInterceptorLogger(logger))
                .setLevel(HttpLoggingInterceptor.Level.BODY)
        )
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

    private fun createCatchAllInterceptor() = Interceptor {
        // OkHttp crashed the app if an other Exception than IOException is thrown. Let's not do that....
        var url = "undefined"
        try {
            url = it.request().url.toString()
            it.proceed(it.request())
        } catch (e: IOException) {
            // IOException will be caught
            throw e
        } catch (e: Throwable) {
            // Let's wrap in OctoprintException (extends IOException)
            throw OctoPrintException(e, "Uncaught exception while requesting: $url")
        }
    }
}