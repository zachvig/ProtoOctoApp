package de.crysxd.octoapp.octoprint.exceptions

import okhttp3.Interceptor
import okhttp3.Response

class GenerateExceptionInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())

        when (response.code()) {
            409 -> throw PrinterNotOperationalException()
            403 -> throw InvalidApiKeyException()
        }

        return response
    }
}