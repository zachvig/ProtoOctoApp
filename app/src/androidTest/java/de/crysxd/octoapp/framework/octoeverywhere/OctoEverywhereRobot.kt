package de.crysxd.octoapp.framework.octoeverywhere

import de.crysxd.octoapp.BuildConfig.TEST_OCTOEVERYWHERE_PASSWORD
import de.crysxd.octoapp.BuildConfig.TEST_OCTOEVERYWHERE_USER
import kotlinx.coroutines.runBlocking
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import timber.log.Timber

object OctoEverywhereRobot {

    private val cookieJar = OctoEverywhereCookieJar()
    private val api = Retrofit.Builder()
        .client(OkHttpClient.Builder().cookieJar(cookieJar).addInterceptor(HttpLoggingInterceptor().also { it.level = HttpLoggingInterceptor.Level.BASIC }).build())
        .baseUrl("https://octoeverywhere.com/api/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(OctoEverywhereAdminApi::class.java)

    fun setPremiumAccountActive(active: Boolean) = runBlocking {
        if (cookieJar.isEmpty()) {
            Timber.i("OctoEverywhere test is using user '$TEST_OCTOEVERYWHERE_USER'")
            api.logIn(
                OctoEverywhereAdminLoginBody(
                    email = TEST_OCTOEVERYWHERE_USER,
                    password = TEST_OCTOEVERYWHERE_PASSWORD
                )
            )
        }

        api.toggleFreeTrial(active.toString())
    }

    private interface OctoEverywhereAdminApi {

        @POST("user/login")
        suspend fun logIn(@Body body: OctoEverywhereAdminLoginBody)

        @GET("user/togglefreetrial")
        suspend fun toggleFreeTrial(@Query("enable") enabled: String)
    }

    private class OctoEverywhereCookieJar : CookieJar {
        private var cookies: List<Cookie> = listOf()

        fun isEmpty() = cookies.isEmpty()

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            this.cookies = cookies
        }

        override fun loadForRequest(url: HttpUrl) = cookies
    }
}