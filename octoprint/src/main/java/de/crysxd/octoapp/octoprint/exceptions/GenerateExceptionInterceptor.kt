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
                409 -> throw PrinterNotOperationalException()
                403 -> throw InvalidApiKeyException()
                in 500..599 -> throw OctoprintBootingException()
            }
            return response
        } catch (e: ConnectException) {
            throw OctoprintUnavailableException(e)
        } catch (e: HttpException) {
            throw OctoprintUnavailableException(e)
        }
    }
}