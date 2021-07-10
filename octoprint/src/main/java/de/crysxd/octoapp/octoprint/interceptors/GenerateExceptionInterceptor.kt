package de.crysxd.octoapp.octoprint.interceptors

import de.crysxd.octoapp.octoprint.api.UserApi
import de.crysxd.octoapp.octoprint.exceptions.*
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.security.cert.CertPathValidatorException
import java.util.regex.Pattern
import javax.net.ssl.SSLHandshakeException

class GenerateExceptionInterceptor(
    private val networkExceptionListener: ((Exception) -> Unit)?,
    private val userApiFactory: (() -> UserApi)?,
) : Interceptor {

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
                    403 -> throw generate403Exception(response)
                    413 -> throw generate413Exception(response)
                    in 501..599 -> throw OctoPrintBootingException()

                    // OctoEverywhere
                    601 -> throw OctoEverywhereCantReachPrinterException()
                    603, 604, 606 -> throw OctoEverywhereConnectionNotFoundException()
                    605 -> throw OctoEverywhereSubscriptionMissingException()
                    607 -> throw generate413Exception(response)

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
            networkExceptionListener?.invoke(e)
            throw e
        }
    }

    private fun generate403Exception(response: Response): Exception = runBlocking {
        // Prevent a loop. We will below request the /currentuser endpoint to test the API key
        val invalidApiKeyException = InvalidApiKeyException(response.request.url)
        if (response.request.url.pathSegments.last() == "currentuser" || userApiFactory == null) return@runBlocking invalidApiKeyException

        // We don't know what caused the 403. Requesting the currentuser will tell us whether we are a guest, meaning the API
        // key is not valid. If we are not a guest, 403 indicates a missing permission
        val isGuest = userApiFactory.invoke().getCurrentUser().isGuest
        if (isGuest) {
            invalidApiKeyException
        } else {
            MissingPermissionException(response.request.url)
        }
    }

    private fun generate413Exception(response: Response) = OctoPrintException(
        userFacingMessage = "The server does not allow downloading this file because it is too large.",
        technicalMessage = "Received response code 413, indicating content is too large",
        webUrl = response.request.url.toString(),
        apiKey = null,
    )

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