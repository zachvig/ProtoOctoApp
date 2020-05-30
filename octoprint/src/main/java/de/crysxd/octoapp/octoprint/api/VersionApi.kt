package de.crysxd.octoapp.octoprint.api

import de.crysxd.octoapp.octoprint.models.version.VersionInfo
import retrofit2.http.GET


interface VersionApi {

    @GET("version")
    suspend fun getVersion(): VersionInfo

}