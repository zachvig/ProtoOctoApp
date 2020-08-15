package de.crysxd.octoapp.octoprint.exceptions

import okhttp3.Interceptor
import okhttp3.Response
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException

class GenerateExceptionInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        try {
            val response = try {
                chain.proceed(chain.request())
            } catch (e: SocketTimeoutException) {
                throw IOException("Caught exception in response to ${chain.request().url}", e)
            }

            return when (response.code) {
                101 -> response
                in 200..204 -> response
                409 -> throw PrinterNotOperationalException(response.request.url)
                403 -> throw InvalidApiKeyException(response.request.url)
                in 501..599 -> throw OctoPrintBootingException()
                else -> throw OctoPrintApiException(response.request.url, response.code, response.body?.string() ?: "<empty>")
            }
        } catch (e: ConnectException) {
            throw OctoPrintUnavailableException(e)
        } catch (e: HttpException) {
            throw OctoPrintUnavailableException(e)
        }
    }
}