package de.crysxd.octoapp.octoprint.interceptors

import de.crysxd.octoapp.octoprint.exceptions.OctoPrintException
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class CatchAllInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        // OkHttp crashed the app if an other Exception than IOException is thrown. Let's not do that....
        var url = "undefined"
        return try {
            url = chain.request().url.toString()
            chain.proceed(chain.request())
        } catch (e: IOException) {
            // IOException will be caught
            throw e
        } catch (e: Throwable) {
            // Let's wrap in OctoprintException (extends IOException)
            throw OctoPrintException(e, "Uncaught exception while requesting: $url")
        }
    }
}

