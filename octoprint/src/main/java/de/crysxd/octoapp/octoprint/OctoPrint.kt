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
import de.crysxd.octoapp.octoprint.plugins.companion.OctoAppCompanionApi
import de.crysxd.octoapp.octoprint.plugins.companion.OctoAppCompanionApiWrapper
import de.crysxd.octoapp.octoprint.plugins.materialmanager.MaterialManagerPluginsCollection
import de.crysxd.octoapp.octoprint.plugins.octoeverywhere.OctoEverywhereApi
import de.crysxd.octoapp.octoprint.plugins.pluginmanager.PluginManagerApi
import de.crysxd.octoapp.octoprint.plugins.power.PowerPluginsCollection
import de.crysxd.octoapp.octoprint.plugins.thespaghettidetective.SpaghettiDetectiveApi
import de.crysxd.octoapp.octoprint.plugins.thespaghettidetective.SpaghettiDetectiveApiWrapper
import de.crysxd.octoapp.octoprint.websocket.ContinuousOnlineCheck
import de.crysxd.octoapp.octoprint.websocket.EventWebSocket
import okhttp3.Dns
import okhttp3.EventListener
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.KeyStore
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger
import javax.net.ssl.*


class OctoPrint(
    val id: String?,
    private val rawWebUrl: HttpUrl,
    private val rawAlternativeWebUrl: HttpUrl?,
    private val apiKey: String,
    private val highLevelInterceptors: List<Interceptor> = emptyList(),
    private val customDns: Dns? = null,
    private val keyStore: KeyStore? = null,
    private val hostnameVerifier: HostnameVerifier? = null,
    private val networkExceptionListener: (Exception) -> Unit = { },
    private val httpEventListener: EventListener? = null,
    val readWriteTimeout: Long = 5000,
    val connectTimeoutMs: Long = 10000,
    val webSocketConnectionTimeout: Long = 5000,
    val webSocketPingPongTimeout: Long = 5000,
    private val debug: Boolean,
) {

    val webUrl = rawWebUrl.withoutBasicAuth()
    private val alternativeWebUrl = rawAlternativeWebUrl?.withoutBasicAuth()
    private val alternativeWebUrlInterceptor = AlternativeWebUrlInterceptor(
        logger = createHttpLogger(),
        fullWebUrl = rawWebUrl,
        fullAlternativeWebUrl = rawAlternativeWebUrl
    )
    private val continuousOnlineCheck = ContinuousOnlineCheck(
        url = webUrl,
        localDns = customDns,
        logger = getLogger(),
        onOnline = {
            if (!alternativeWebUrlInterceptor.isPrimaryUsed) {
                getLogger().log(Level.INFO, "Switching back to primary web url")
                alternativeWebUrlInterceptor.isPrimaryUsed = true
                webSocket.reconnect()
            }
        }
    )
    val activeUrl get() = alternativeWebUrlInterceptor.activeUrl
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

    // For the probe API we use a call timeout to prevent the app from being "stuck".
    // We don't expect large downloads here, so using a call timeout is fine
    suspend fun probeConnection() = createRetrofit(
        path = ".",
        okHttpClient = okHttpClient.newBuilder().callTimeout(readWriteTimeout, TimeUnit.MILLISECONDS).build()
    ).create(ProbeApi::class.java).probe().code()

    fun createUserApi(retrofit: Retrofit = createRetrofit()): UserApi =
        retrofit.create(UserApi::class.java)

    fun createLoginApi(): LoginApi =
        createRetrofit().create(LoginApi::class.java)

    fun createVersionApi(): VersionApi =
        createRetrofit().create(VersionApi::class.java)

    fun createSettingsApi(): SettingsApi =
        createRetrofit().create(SettingsApi::class.java)

    fun createPluginManagerApi(): PluginManagerApi =
        createRetrofit(".").create(PluginManagerApi::class.java)

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

    fun createTimelapseApi(): TimelapseApi.Wrapper =
        TimelapseApi.Wrapper(createRetrofit().create(TimelapseApi::class.java))

    fun createPowerPluginsCollection() = PowerPluginsCollection(createRetrofit())

    fun createMaterialManagerPluginsCollection() = MaterialManagerPluginsCollection(createRetrofit("."))

    fun createConnectionApi(): ConnectionApi.Wrapper =
        ConnectionApi.Wrapper((createRetrofit().create(ConnectionApi::class.java)))

    fun createApplicationKeysPluginApi(): ApplicationKeysPluginApi.Wrapper =
        ApplicationKeysPluginApi.Wrapper((createRetrofit(".").create(ApplicationKeysPluginApi::class.java)))

    fun createSystemApi(): SystemApi.Wrapper =
        SystemApi.Wrapper((createRetrofit().create(SystemApi::class.java)))

    fun createOctoEverywhereApi() = createRetrofit().create(OctoEverywhereApi::class.java)

    fun createOctoAppCompanionApi() = OctoAppCompanionApiWrapper(createRetrofit().create(OctoAppCompanionApi::class.java))

    fun createSpaghettiDetectiveApi() = SpaghettiDetectiveApiWrapper(createRetrofit().create(SpaghettiDetectiveApi::class.java))

    fun getLogger(): Logger = OctoPrintLogger

    private fun createHttpLogger(): Logger {
        val logger = Logger.getLogger("OctoPrint/HTTP")
        logger.parent = getLogger()
        logger.useParentHandlers = true
        return logger
    }

    private fun createRetrofit(path: String = "api/", okHttpClient: OkHttpClient = this.okHttpClient) = Retrofit.Builder()
        .baseUrl(webUrl.resolvePath(path))
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

    fun createOkHttpClient(): OkHttpClient = OkHttpClient.Builder().apply {
        val logger = createHttpLogger()
        httpEventListener?.let(::eventListener)

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
        addInterceptor(GenerateExceptionInterceptor(networkExceptionListener) {
            createUserApi(createRetrofit(okHttpClient = createOkHttpClient()))
        })

        // 5. This interceptor consumes raw IOException and might switch the host
        addInterceptor(alternativeWebUrlInterceptor)

        // 7. Basic Auth interceptor is the last because we might change the host above
        addInterceptor(BasicAuthInterceptor(logger, rawWebUrl, rawAlternativeWebUrl))

        // 8. Logger needs to be lowest level, we need to log any change made in the stack above
        addInterceptor(
            HttpLoggingInterceptor(LoggingInterceptorLogger(logger))
                .setLevel(if (debug) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.HEADERS)
        )
    }.build()
}