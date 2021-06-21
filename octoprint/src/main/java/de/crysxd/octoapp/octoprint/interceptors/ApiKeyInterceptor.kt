package de.crysxd.octoapp.octoprint.interceptors

import okhttp3.Interceptor

class ApiKeyInterceptor(private val apiKey: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain) = chain.proceed(
        chain.request().newBuilder().also {
            if (apiKey.isNotBlank()) {
                it.addHeader("X-Api-Key", apiKey)
            }
            it.addHeader("Accept", "application/json")
        }.build()
    )
}