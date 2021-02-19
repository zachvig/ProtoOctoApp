package de.crysxd.octoapp.octoprint.interceptors

import de.crysxd.octoapp.octoprint.exceptions.OctoPrintException
import de.crysxd.octoapp.octoprint.exceptions.ProxyException
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

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
        } catch (e: ProxyException) {
            // ProxyException are already free of sensitive data
            throw e
        } catch (e: OctoPrintException) {
            // OctoPrintException are guaranteed to not contain sensitive data, we can throw them up right away
            throw e
        } catch (e: IOException) {
            // We create a "proxy exception" as the first line of defense against data leaks.
            // The proxy exception will have the original stack trace but the message will be scrubbed to remove sensitive data
            throw ProxyException(e, baseUrl, apiKey)
        } catch (e: Throwable) {
            // Let's wrap in OctoprintException (extends IOException)
            throw OctoPrintException(ProxyException(e, baseUrl, apiKey), ProxyException.mask("Uncaught exception while requesting: $url", baseUrl, apiKey))
        }
    }
}

