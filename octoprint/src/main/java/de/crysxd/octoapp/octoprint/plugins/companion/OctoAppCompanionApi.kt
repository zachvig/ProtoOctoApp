package de.crysxd.octoapp.octoprint.plugins.companion

import de.crysxd.octoapp.octoprint.exceptions.OctoPrintApiException
import retrofit2.http.Body
import retrofit2.http.POST

interface OctoAppCompanionApi {

    @POST("plugin/octoapp")
    suspend fun registerApp(@Body registrationBody: AppRegistrationBody)

    @POST("plugin/octoapp")
    suspend fun getFirmwareInfo(@Body commandBody: GetFirmwareInfoBody): Map<String, String>
}

class OctoAppCompanionApiWrapper(private val wrapped: OctoAppCompanionApi) {

    suspend fun registerApp(registrationBody: AppRegistrationBody) = wrapped.registerApp(registrationBody)

    suspend fun getFirmwareInfo() = try {
        wrapped.getFirmwareInfo(GetFirmwareInfoBody()).map { "${it.key}: ${it.value}" }.joinToString(" ")
    } catch (e: OctoPrintApiException) {
        // 400 indicates that the plugin version does not yet have this command implemented
        if (e.responseCode == 400) {
            null
        } else {
            throw e
        }
    }
}
