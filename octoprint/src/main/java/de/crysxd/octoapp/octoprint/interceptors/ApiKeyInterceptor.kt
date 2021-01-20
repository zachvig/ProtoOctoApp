package de.crysxd.octoapp.octoprint.interceptors

import okhttp3.Interceptor

class ApiKeyInterceptor(private val apiKey: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain) = chain.proceed(
        chain.request().newBuilder()
            .addHeader("X-Api-Key", apiKey)
            .addHeader("Accept", "application/json")
            .build()
    )
}