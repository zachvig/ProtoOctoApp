package de.crysxd.octoapp.octoprint.api

import de.crysxd.octoapp.octoprint.models.settings.Settings
import retrofit2.http.GET

interface SettingsApi {

    @GET("settings")
    suspend fun getSettings(): Settings

}