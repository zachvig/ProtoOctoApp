package de.crysxd.octoapp.octoprint.api

import de.crysxd.octoapp.octoprint.models.user.User
import retrofit2.http.GET

interface UserApi {

    @GET("currentuser")
    suspend fun getCurrentUser(): User
}