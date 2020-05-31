package de.crysxd.octoapp.octoprint

import de.crysxd.octoapp.octoprint.api.PrinterApi
import de.crysxd.octoapp.octoprint.api.VersionApi
import de.crysxd.octoapp.octoprint.exceptions.GenerateExceptionInterceptor
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class OctoPrint (
    private val hostName: String,
    private val port: Int,
    private val apiKey: String,
    private val interceptors: List<Interceptor> = emptyList()
) {

    fun createVersionApi(): VersionApi = createRetrofit().create(VersionApi::class.java)

    fun createPrinterApi(): PrinterApi = createRetrofit().create(PrinterApi::class.java)

    private fun createRetrofit() = Retrofit.Builder()
        .baseUrl("http://${hostName}:${port}/api/")
        .addConverterFactory(GsonConverterFactory.create())
        .client(createOkHttpClient())
        .build()

    private fun createOkHttpClient() = OkHttpClient.Builder().apply {
        addInterceptor(createAddHeaderInterceptor())
        addInterceptor(GenerateExceptionInterceptor())
        this@OctoPrint.interceptors.forEach { addInterceptor(it) }
    }.build()

    private fun createAddHeaderInterceptor() = Interceptor {
        it.proceed(it.request().newBuilder()
            .addHeader("X-Api-Key", apiKey)
            .addHeader("Accept-Encoding", "application/json")
            .build())
    }
}