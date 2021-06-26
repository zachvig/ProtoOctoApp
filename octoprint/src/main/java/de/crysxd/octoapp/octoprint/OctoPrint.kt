package de.crysxd.octoapp.octoprint

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import de.crysxd.octoapp.octoprint.api.*
import de.crysxd.octoapp.octoprint.interceptors.*
import de.crysxd.octoapp.octoprint.json.*
import de.crysxd.octoapp.octoprint.logging.LoggingInterceptorLogger
import de.crysxd.octoapp.octoprint.models.connection.ConnectionResponse
import de.crysxd.octoapp.octoprint.models.files.FileObject
import de.crysxd.octoapp.octoprint.models.job.ProgressInformation
import de.crysxd.octoapp.octoprint.models.settings.Settings
import de.crysxd.octoapp.octoprint.models.socket.HistoricTemperatureData
import de.crysxd.octoapp.octoprint.models.socket.Message
import de.crysxd.octoapp.octoprint.plugins.applicationkeys.ApplicationKeysPluginApi
import de.crysxd.octoapp.octoprint.plugins.materialmanager.MaterialManagerPluginsCollection
import de.crysxd.octoapp.octoprint.plugins.octoeverywhere.OctoEverywhereApi
import de.crysxd.octoapp.octoprint.plugins.power.PowerPluginsCollection
import de.crysxd.octoapp.octoprint.websocket.ContinuousOnlineCheck
import de.crysxd.octoapp.octoprint.websocket.EventWebSocket
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URI
import java.security.KeyStore
import java.security.SecureRandom
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger
import javax.net.ssl.*


class OctoPrint(
    rawWebUrl: UrlString,
    rawAlternativeWebUrl: UrlString?,
    private val apiKey: String,
    private val interceptors: List<Interceptor> = emptyList(),
    private val keyStore: KeyStore? = null,
    private val hostnameVerifier: HostnameVerifier? = null,
    private val networkExceptionListener: (Exception) -> Unit = { },
    val readWriteTimeout: Long = 5000,
    val connectTimeoutMs: Long = 10000,
    val webSocketConnectionTimeout: Long = 5000,
    val webSocketPingPongTimeout: Long = 5000,
) {

    val fullWebUrl = rawWebUrl.sanitizeUrl()
    val fullAlternativeWebUrl = rawAlternativeWebUrl?.sanitizeUrl()
    val webUrl = rawWebUrl.removeUserInfo().sanitizeUrl()
    private val alternativeWebUrl = rawAlternativeWebUrl?.removeUserInfo()?.sanitizeUrl()
    private val alternativeWebUrlInterceptor = AlternativeWebUrlInterceptor(createHttpLogger(), webUrl, alternativeWebUrl)
    private val continuousOnlineCheck = ContinuousOnlineCheck(
        url = webUrl,
        logger = createHttpLogger(),
        onOnline = {
            if (!alternativeWebUrlInterceptor.isPrimaryUsed) {
                getLogger().log(Level.INFO, "Switching back to primary web url")
                alternativeWebUrlInterceptor.isPrimaryUsed = true
                webSocket.reconnect()
            }
        }
    )
    val isAlternativeUrlBeingUsed get() = !alternativeWebUrlInterceptor.isPrimaryUsed

    private val webSocket = EventWebSocket(
        httpClient = createOkHttpClient(),
        webUrl = webUrl,
        getCurrentConnectionType = { alternativeWebUrlInterceptor.getActiveConnectionType() },
        gson = createGsonWithTypeAdapters(),
        logger = getLogger(),
        loginApi = createLoginApi(),
        onStart = ::startOnlineCheck,
        onStop = ::stopOnlineCheck,
        pingPongTimeoutMs = webSocketPingPongTimeout,
        connectionTimeoutMs = webSocketConnectionTimeout
    )

    fun performOnlineCheck() {
        continuousOnlineCheck.checkNow()
    }

    private fun startOnlineCheck() {
        continuousOnlineCheck.start()
    }

    private fun stopOnlineCheck() {
        continuousOnlineCheck.stop()
    }

    fun getEventWebSocket() = webSocket

    suspend fun probeConnection() = createRetrofit(".").create(ProbeApi::class.java).probe()

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
        PrinterApi.Wrapper(createRetrofit().create(PrinterApi::class.java))

    fun createPowerPluginsCollection() = PowerPluginsCollection(createRetrofit())

    fun createMaterialManagerPluginsCollection() = MaterialManagerPluginsCollection(createRetrofit("."))

    fun createConnectionApi(): ConnectionApi.Wrapper =
        ConnectionApi.Wrapper((createRetrofit().create(ConnectionApi::class.java)))

    fun createApplicationKeysPluginApi(): ApplicationKeysPluginApi.Wrapper =
        ApplicationKeysPluginApi.Wrapper((createRetrofit().create(ApplicationKeysPluginApi::class.java)))

    fun createSystemApi(): SystemApi.Wrapper =
        SystemApi.Wrapper((createRetrofit().create(SystemApi::class.java)))

    fun createOctoEverywhereApi() = createRetrofit().create(OctoEverywhereApi::class.java)

    fun getLogger(): Logger = Logger.getLogger("OctoPrint")

    private fun createHttpLogger(): Logger {
        val logger = Logger.getLogger("OctoPrint/HTTP")
        logger.parent = getLogger()
        logger.useParentHandlers = true
        return logger
    }

    private fun createRetrofit(path: String = "api/") = Retrofit.Builder()
        .baseUrl(URI.create(webUrl).resolve(path).toURL())
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
        .registerTypeAdapter(HistoricTemperatureData::class.java, HistoricTemperatureDeserializer())
        .registerTypeAdapter(ProgressInformation::class.java, ProgressInformationDeserializer(Gson()))
        .create()

    fun createOkHttpClient() = OkHttpClient.Builder().apply {
        val logger = createHttpLogger()

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

        addInterceptor(CatchAllInterceptor(webUrl, apiKey))
        addInterceptor(ApiKeyInterceptor(apiKey))
        addInterceptor(GenerateExceptionInterceptor(networkExceptionListener))
        addInterceptor(alternativeWebUrlInterceptor)
        addInterceptor(BasicAuthInterceptor(logger, fullWebUrl, fullAlternativeWebUrl))
        connectTimeout(connectTimeoutMs, TimeUnit.MILLISECONDS)
        readTimeout(readWriteTimeout, TimeUnit.MILLISECONDS)
        writeTimeout(readWriteTimeout, TimeUnit.MILLISECONDS)
        addInterceptor(
            HttpLoggingInterceptor(LoggingInterceptorLogger(logger))
                .setLevel(HttpLoggingInterceptor.Level.HEADERS)
        )
        this@OctoPrint.interceptors.forEach { addInterceptor(it) }
    }.build()
}