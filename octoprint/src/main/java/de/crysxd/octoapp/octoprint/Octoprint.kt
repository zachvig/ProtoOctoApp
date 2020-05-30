package de.crysxd.octoapp.octoprint

import de.crysxd.octoapp.octoprint.api.VersionApi
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class Octoprint (
    private val hostName: String,
    private val port: Int,
    private val apiKey: String,
    private val interceptors: List<Interceptor> = emptyList()
) {

    fun createVersionApi() = createRetrofit().create(VersionApi::class.java)

    private fun createRetrofit() = Retrofit.Builder()
        .baseUrl("http://${hostName}:${port}/api/")
        .addConverterFactory(GsonConverterFactory.create())
        .client(createOkHttpClient())
        .build()

    private fun createOkHttpClient() = OkHttpClient.Builder().apply {
        this@Octoprint.interceptors.forEach { addInterceptor(it) }
        addInterceptor(createAddHeaderInterceptor())
    }.build()

    private fun createAddHeaderInterceptor() = Interceptor {
        it.proceed(it.request().newBuilder()
            .addHeader("X-Api-Key", apiKey)
            .addHeader("Accept-Encoding", "application/json")
            .build())
    }
}