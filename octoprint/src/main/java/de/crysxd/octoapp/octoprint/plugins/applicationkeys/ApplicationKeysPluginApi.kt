package de.crysxd.octoapp.octoprint.plugins.applicationkeys

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApplicationKeysPluginApi {

    @GET("plugin/appkeys/probe")
    suspend fun probe(): Response<Unit>

    @POST("plugin/appkeys/request")
    suspend fun request(@Body body: RequestBody): RequestResponse

    @GET("plugin/appkeys/request/{appToken}")
    suspend fun checkStatus(@Path("appToken") appToken: String): Response<CheckResponse?>

    class Wrapper(private val wrapped: ApplicationKeysPluginApi) {

        suspend fun probe() = try {
            wrapped.probe().code() == 204
        } catch (e: Exception) {
            false
        }

        suspend fun request(appName: String) = wrapped.request(RequestBody(appName))

        suspend fun checkStatus(res: RequestResponse): RequestStatus {
            val result = wrapped.checkStatus(res.appToken)
            val body = result.body()
            return when {
                result.code() == 404 -> RequestStatus.DeniedOrTimedOut
                result.code() == 202 -> RequestStatus.Pending
                body != null -> RequestStatus.Granted(apiKey = body.apiKey)
                else -> throw IllegalStateException("Unknown request state (${result.code()})")
            }
        }
    }
}