package de.crysxd.octoapp.octoprint

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import de.crysxd.octoapp.octoprint.api.FilesApi
import de.crysxd.octoapp.octoprint.api.PrinterApi
import de.crysxd.octoapp.octoprint.api.PsuApi
import de.crysxd.octoapp.octoprint.api.VersionApi
import de.crysxd.octoapp.octoprint.exceptions.GenerateExceptionInterceptor
import de.crysxd.octoapp.octoprint.json.FileObjectDeserializer
import de.crysxd.octoapp.octoprint.models.files.FileObject
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

    fun createVersionApi(): VersionApi =
        createRetrofit().create(VersionApi::class.java)

    fun createFilesApi(): FilesApi =
        createRetrofit().create(FilesApi::class.java)

    fun createPrinterApi(): PrinterApi.Wrapper =
        PrinterApi.Wrapper(createRetrofit().create(PrinterApi::class.java))

    fun createPsuApi(): PsuApi.Wrapper =
        PsuApi.Wrapper((createRetrofit().create(PsuApi::class.java)))

    private fun createRetrofit() = Retrofit.Builder()
        .baseUrl("http://${hostName}:${port}/api/")
        .addConverterFactory(GsonConverterFactory.create(createGsonWithTypeAdapters()))
        .client(createOkHttpClient())
        .build()

    private fun createGsonWithTypeAdapters(): Gson = createBaseGson().newBuilder()
        .registerTypeAdapter(FileObject::class.java, FileObjectDeserializer(createBaseGson()))
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