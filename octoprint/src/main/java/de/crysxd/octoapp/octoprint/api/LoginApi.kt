package de.crysxd.octoapp.octoprint.api

import de.crysxd.octoapp.octoprint.models.login.LoginResponse
import retrofit2.http.POST

interface LoginApi {

    @POST("/api/login?passive")
    suspend fun passiveLogin(): LoginResponse

}