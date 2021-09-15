package de.crysxd.octoapp.octoprint.plugins.companion

import retrofit2.http.Body
import retrofit2.http.POST

interface OctoAppCompanionApi {

    @POST("plugin/octoapp")
    suspend fun registerApp(@Body registrationBody: AppRegistrationBody)
}
