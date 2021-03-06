package de.crysxd.octoapp.octoprint.interceptors

import de.crysxd.octoapp.octoprint.exceptions.OctoPrintException
import okhttp3.Interceptor
import okhttp3.Response

class CatchAllInterceptor(
    val baseUrl: String,
    val apiKey: String
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        // OkHttp crashed the app if an other Exception than IOException is thrown. Let's not do that....
        var url = "undefined"
        return try {
            url = chain.request().url.toString()
            chain.proceed(chain.request())
        } catch (e: OctoPrintException) {
            // OctoPrintException are guaranteed to not contain sensitive data, we can throw them up right away
            throw e
        } catch (e: Throwable) {
            // Let's wrap in OctoprintException (extends IOException)
            throw OctoPrintException(
                originalCause = e,
                technicalMessage = "Uncaught exception while requesting: $url",
                apiKey = apiKey,
                webUrl = url
            )
        }
    }
}

