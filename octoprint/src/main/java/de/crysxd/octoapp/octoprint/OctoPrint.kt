package de.crysxd.octoapp.octoprint

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import de.crysxd.octoapp.octoprint.api.*
import de.crysxd.octoapp.octoprint.ext.withHostnameVerifier
import de.crysxd.octoapp.octoprint.ext.withSslKeystore
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
import okhttp3.Dns
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URI
import java.security.KeyStore
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger
import javax.net.ssl.*


class OctoPrint(
    rawWebUrl: UrlString,
    rawAlternativeWebUrl: UrlString?,
    private val apiKey: String,
    private val highLevelInterceptors: List<Interceptor> = emptyList(),
    private val customDns: Dns? = null,
    private val keyStore: KeyStore? = null,
    private val hostnameVerifier: HostnameVerifier? = null,
    private val networkExceptionListener: (Exception) -> Unit = { },
    val readWriteTimeout: Long = 5000,
    val connectTimeoutMs: Long = 10000,
    val webSocketConnectionTimeout: Long = 5000,
    val webSocketPingPongTimeout: Long = 5000,
    private val debug: Boolean,
) {

    val fullWebUrl = rawWebUrl.sanitizeUrl()
    val fullAlternativeWebUrl = rawAlternativeWebUrl?.sanitizeUrl()
    val webUrl = rawWebUrl.removeUserInfo().sanitizeUrl()
    private val alternativeWebUrl = rawAlternativeWebUrl?.removeUserInfo()?.sanitizeUrl()
    private val alternativeWebUrlInterceptor = AlternativeWebUrlInterceptor(createHttpLogger(), webUrl, alternativeWebUrl)
    private val continuousOnlineCheck = ContinuousOnlineCheck(
        url = webUrl,
        localDns = customDns,
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
    private val okHttpClient = createOkHttpClient()

    private val webSocket = EventWebSocket(
        httpClient = okHttpClient,
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

    suspend fun probeConnection() = createRetrofit(".").create(ProbeApi::class.java).probe().code()

    fun createUserApi(): UserApi =
        createRetrofit().create(UserApi::class.java)

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
            okHttpClient = okHttpClient,
            wrapped = createRetrofit().create(FilesApi::class.java)
        )

    fun createPrinterApi(): PrinterApi.Wrapper =
        PrinterApi.Wrapper(createRetrofit().create(PrinterApi::class.java))

    fun createPowerPluginsCollection() = PowerPluginsCollection(createRetrofit())

    fun createMaterialManagerPluginsCollection() = MaterialManagerPluginsCollection(createRetrofit("."))

    fun createConnectionApi(): ConnectionApi.Wrapper =
        ConnectionApi.Wrapper((createRetrofit().create(ConnectionApi::class.java)))

    fun createApplicationKeysPluginApi(): ApplicationKeysPluginApi.Wrapper =
        ApplicationKeysPluginApi.Wrapper((createRetrofit(".").create(ApplicationKeysPluginApi::class.java)))

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
        .client(okHttpClient)
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

        withHostnameVerifier(hostnameVerifier)
        withSslKeystore(keyStore)
        connectTimeout(connectTimeoutMs, TimeUnit.MILLISECONDS)
        readTimeout(readWriteTimeout, TimeUnit.MILLISECONDS)
        writeTimeout(readWriteTimeout, TimeUnit.MILLISECONDS)
        customDns?.let { dns(it) }

        // 1. Add Catch-all interceptor. Uncaught exceptions other than IO lead to a crash,
        // so we wrap any non-IOException in an IOException
        addInterceptor(CatchAllInterceptor(webUrl, apiKey))

        // 2. Add plug-in high level interceptors next
        this@OctoPrint.highLevelInterceptors.forEach { addInterceptor(it) }

        // 3. Sets the API key header
        addInterceptor(ApiKeyInterceptor(apiKey))

        // 4. Consumes raw exceptions and throws wrapped exceptions
        addInterceptor(GenerateExceptionInterceptor(networkExceptionListener) { createUserApi() })

        // 5. This interceptor consumes raw IOException and might switch the host
        addInterceptor(alternativeWebUrlInterceptor)

        // 7. Basic Auth interceptor is the last because we might change the host above
        addInterceptor(BasicAuthInterceptor(logger, fullWebUrl, fullAlternativeWebUrl))

        // 8. Logger needs to be lowest level, we need to log any change made in the stack above
        addInterceptor(
            HttpLoggingInterceptor(LoggingInterceptorLogger(logger))
                .setLevel(if (debug) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.HEADERS)
        )
    }.build()
}