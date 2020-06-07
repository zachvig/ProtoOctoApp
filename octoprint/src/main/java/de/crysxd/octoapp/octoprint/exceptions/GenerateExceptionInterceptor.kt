package de.crysxd.octoapp.octoprint.exceptions

import okhttp3.Interceptor
import okhttp3.Response
import retrofit2.HttpException
import java.net.ConnectException

class GenerateExceptionInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        try {
            val response = chain.proceed(chain.request())
            when (response.code()) {
                101 -> return response
                in 200..204 ->  return response
                409 -> throw PrinterNotOperationalException()
                403 -> throw InvalidApiKeyException()
                in 501..599 -> throw OctoPrintBootingException()
                else -> throw OctoPrintApiException(response.code())
            }
        } catch (e: ConnectException) {
            throw OctoPrintUnavailableException(e)
        } catch (e: HttpException) {
            throw OctoPrintUnavailableException(e)
        }
    }
}