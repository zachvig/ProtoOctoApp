package de.crysxd.octoapp.octoprint.interceptors

import de.crysxd.octoapp.octoprint.exceptions.*
import okhttp3.Interceptor
import okhttp3.Response
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.security.cert.CertPathValidatorException
import java.util.regex.Pattern
import javax.net.ssl.SSLHandshakeException

class GenerateExceptionInterceptor(private val networkExceptionListener: (Exception) -> Unit) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        try {
            try {
                val response = try {
                    chain.proceed(request)
                } catch (e: SocketTimeoutException) {
                    throw IOException("Caught exception in response to ${chain.request().url}", e)
                }

                return when (response.code) {
                    // OctoPrint / Generic
                    101 -> response
                    in 200..204 -> response
                    409 -> throw PrinterNotOperationalException(request.url)
                    401 -> throw generate401Exception(response)
                    403 -> throw InvalidApiKeyException(request.url)
                    in 501..599 -> throw OctoPrintBootingException()

                    // OctoEverywhere
                    601 -> throw OctoEverywhereCantReachPrinterException()
                    603, 604, 606 -> throw OctoEverywhereConnectionNotFoundException()
                    605 -> throw OctoEverywhereSubscriptionMissingException()

                    else -> throw generateGenericException(response)
                }
            } catch (e: ConnectException) {
                throw OctoPrintUnavailableException(e, request.url)
            } catch (e: HttpException) {
                throw OctoPrintUnavailableException(e, request.url)
            } catch (e: SSLHandshakeException) {
                throw OctoPrintHttpsException(request.url, e)
            } catch (e: CertPathValidatorException) {
                throw OctoPrintHttpsException(request.url, e)
            }
        } catch (e: Exception) {
            networkExceptionListener(e)
            throw e
        }
    }

    private fun generate401Exception(response: Response): IOException {
        val authHeader = response.headers["WWW-Authenticate"]
        return authHeader?.let {
            val realmMatcher = Pattern.compile("realm=\"([^\"]*)\"").matcher(it)
            if (realmMatcher.find()) {
                BasicAuthRequiredException(realmMatcher.group(1))
            } else {
                BasicAuthRequiredException("no message")
            }
        } ?: generateGenericException(response)
    }

    private fun generateGenericException(response: Response): IOException =
        OctoPrintApiException(response.request.url, response.code, response.body?.string() ?: "<empty>")
}