package de.crysxd.octoapp.octoprint

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import de.crysxd.octoapp.octoprint.api.*
import de.crysxd.octoapp.octoprint.exceptions.GenerateExceptionInterceptor
import de.crysxd.octoapp.octoprint.interceptors.ApiKeyInterceptor
import de.crysxd.octoapp.octoprint.interceptors.BasicAuthInterceptor
import de.crysxd.octoapp.octoprint.interceptors.CatchAllInterceptor
import de.crysxd.octoapp.octoprint.json.ConnectionStateDeserializer
import de.crysxd.octoapp.octoprint.json.FileObjectDeserializer
import de.crysxd.octoapp.octoprint.json.MessageDeserializer
import de.crysxd.octoapp.octoprint.json.PluginSettingsDeserializer
import de.crysxd.octoapp.octoprint.logging.LoggingInterceptorLogger
import de.crysxd.octoapp.octoprint.models.connection.ConnectionResponse
import de.crysxd.octoapp.octoprint.models.files.FileObject
import de.crysxd.octoapp.octoprint.models.settings.Settings
import de.crysxd.octoapp.octoprint.models.socket.Message
import de.crysxd.octoapp.octoprint.plugins.power.PowerPluginsCollection
import de.crysxd.octoapp.octoprint.websocket.EventWebSocket
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URI
import java.security.KeyStore
import java.security.SecureRandom
import java.util.logging.Logger
import javax.net.ssl.*


class OctoPrint(
    rawWebUrl: String,
    private val apiKey: String,
    private val interceptors: List<Interceptor> = emptyList(),
    private val keyStore: KeyStore?,
    private val hostnameVerifier: HostnameVerifier?,
    webSocketPingPongTimeoutMs: Long,
    webSocketConnectTimeoutMs: Long,
) {

    val webUrl = "${rawWebUrl.removeSuffix("/")}/"

    private val webSocket = EventWebSocket(
        httpClient = createOkHttpClient(),
        webUrl = webUrl,
        gson = createGsonWithTypeAdapters(),
        logger = getLogger(),
        loginApi = createLoginApi(),
        pingPongTimeoutMs = webSocketPingPongTimeoutMs,
        connectionTimeoutMs = webSocketConnectTimeoutMs
    )

    fun getEventWebSocket() = webSocket

    fun createLoginApi(): LoginApi =
        createRetrofit().create(LoginApi::class.java)

    fun createVersionApi(): VersionApi =
        createRetrofit().create(VersionApi::class.java)

    fun createSettingsApi(): SettingsApi =
        createRetrofit().create(SettingsApi::class.java)

    fun createPrinterProfileApi(): PrinterProfileApi =
        createRetrofit().create(PrinterProfileApi::class.java)

    fun createJobApi(): JobApi.Wrapper =
        JobApi.Wrapper(createRetrofit().create(JobApi::class.java), webSocket)

    fun createFilesApi(): FilesApi.Wrapper =
        FilesApi.Wrapper(
            webUrl = webUrl,
            okHttpClient = createOkHttpClient(),
            wrapped = createRetrofit().create(FilesApi::class.java)
        )

    fun createPrinterApi(): PrinterApi.Wrapper =
        PrinterApi.Wrapper(createRetrofit().create(PrinterApi::class.java), webSocket)

    fun createPowerPluginsCollection() = PowerPluginsCollection(createRetrofit())

    fun createConnectionApi(): ConnectionApi.Wrapper =
        ConnectionApi.Wrapper((createRetrofit().create(ConnectionApi::class.java)))

    fun createSystemApi(): SystemApi.Wrapper =
        SystemApi.Wrapper((createRetrofit().create(SystemApi::class.java)))

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
        .registerTypeAdapter(Settings.PluginSettingsGroup::class.java, PluginSettingsDeserializer())
        .create()

    private fun createBaseGson(): Gson = GsonBuilder()
        .create()

    fun createOkHttpClient() = OkHttpClient.Builder().apply {
        val logger = Logger.getLogger("OctoPrint/HTTP")
        logger.parent = getLogger()
        logger.useParentHandlers = true

        hostnameVerifier?.let(::hostnameVerifier)
        keyStore?.let { ks ->
            val customTrustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).also {
                it.init(ks)
            }
            val x509TrustManager = customTrustManagerFactory.trustManagers.mapNotNull {
                it as? X509TrustManager
            }.first()

            val sslContext = SSLContext.getInstance("SSL")
            val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
            keyManagerFactory.init(keyStore, "pass".toCharArray())
            sslContext.init(keyManagerFactory.keyManagers, customTrustManagerFactory.trustManagers, SecureRandom())
            sslSocketFactory(sslContext.socketFactory, x509TrustManager)
        }

        addInterceptor(CatchAllInterceptor())
        addInterceptor(BasicAuthInterceptor(webUrl))
        addInterceptor(ApiKeyInterceptor(apiKey))
        addInterceptor(GenerateExceptionInterceptor())
        addInterceptor(
            HttpLoggingInterceptor(LoggingInterceptorLogger(logger))
                .setLevel(HttpLoggingInterceptor.Level.HEADERS)
        )
        this@OctoPrint.interceptors.forEach { addInterceptor(it) }
    }.build()
}