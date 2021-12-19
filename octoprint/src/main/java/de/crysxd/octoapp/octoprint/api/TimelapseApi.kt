package de.crysxd.octoapp.octoprint.api

import de.crysxd.octoapp.octoprint.models.timelapse.TimelapseConfig
import de.crysxd.octoapp.octoprint.models.timelapse.TimelapseFile
import de.crysxd.octoapp.octoprint.models.timelapse.TimelapseStatus
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface TimelapseApi {

    @POST("timelapse")
    suspend fun updateConfig(@Body config: TimelapseConfig): TimelapseStatus

    @GET("timelapse?unrendered=true")
    suspend fun getStatus(): TimelapseStatus

    @DELETE("timelapse/{filename}")
    suspend fun delete(@Path("filename") fileName: String): Response<TimelapseStatus>

    @DELETE("timelapse/unrendered/{filename}")
    suspend fun deleteUnrendered(@Path("filename") fileName: String): Response<TimelapseStatus>

    class Wrapper(private val wrapped: TimelapseApi) {

        suspend fun updateConfig(config: TimelapseConfig) = wrapped.updateConfig(config)
        suspend fun getStatus() = wrapped.getStatus()
        suspend fun delete(timelapseFile: TimelapseFile) {
            requireNotNull(timelapseFile.name)

            // Still processing? Use the unrendered endpoint
            if (timelapseFile.processing == true) {
                // If we get 204 the file wasn't found. Rendering might be completed, also delete on regular endpoint
                if (wrapped.deleteUnrendered(timelapseFile.name).code() == 204) {
                    wrapped.delete(timelapseFile.name)
                }
            }

            // Not rendering anymore, delete on regular endpoint
            else {
                wrapped.delete(timelapseFile.name)
            }
        }
    }
}
