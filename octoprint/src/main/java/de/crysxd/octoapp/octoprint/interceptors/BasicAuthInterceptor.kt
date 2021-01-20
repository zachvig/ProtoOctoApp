package de.crysxd.octoapp.octoprint.interceptors

import de.crysxd.octoapp.octoprint.exceptions.IllegalBasicAuthConfigurationException
import okhttp3.Credentials
import okhttp3.Interceptor
import java.net.URL

class BasicAuthInterceptor(baseUrl: String) : Interceptor {
    private val credentials = URL(baseUrl).userInfo?.let {
        try {
            val components = it.split(":")
            Credentials.basic(components[0], components[1])
        } catch (e: Exception) {
            throw IllegalBasicAuthConfigurationException(baseUrl)
        }
    }

    override fun intercept(chain: Interceptor.Chain) = chain.proceed(
        credentials?.let {
            chain.request().newBuilder()
                .header("Authorization", it)
                .build()
        } ?: chain.request()
    )
}